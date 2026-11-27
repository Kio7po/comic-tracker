import { beforeEach, describe, expect, it, vi } from 'vitest';
import { search } from '@/services/source/api/readingSource';
import type { ComicReadingSearchListing, ComicReadingSource } from '@/services/source/types';
import { ApiError, ProblemType } from '@/common/api';
import { MAX_CONCURRENT_SOURCE_SEARCHES, searchEntryCandidates, type SourceSearchOutcome } from './searchEntryCandidates';

vi.mock('@/services/source/api/readingSource', () => ({ search: vi.fn() }));

const searchMock = vi.mocked(search);

function sourceWithId(id: number): ComicReadingSource {
  return {
    id,
    slug: `source-${id}`,
    name: `Source ${id}`,
    url: `https://source-${id}.example`,
    iconUrl: null,
    status: 'APPROVED',
    createdAt: '2026-01-01T00:00:00Z',
  };
}

function listing(...externalIds: (string | null)[]): ComicReadingSearchListing {
  return {
    providerKey: 'suwayomi',
    results: externalIds.map((externalId, index) => ({
      externalId,
      title: `Title ${index}`,
      url: null,
      coverUrl: null,
    })),
  };
}

async function runSearch(sources: ComicReadingSource[], signal = new AbortController().signal) {
  const outcomes: SourceSearchOutcome[] = [];
  await searchEntryCandidates(sources, 'one piece', (outcome) => outcomes.push(outcome), signal);
  return outcomes;
}

describe('searchEntryCandidates', () => {
  beforeEach(() => {
    searchMock.mockReset();
  });

  it('reports the candidates of a source together with the provider that ran the search', async () => {
    searchMock.mockResolvedValue(listing('731'));

    const outcomes = await runSearch([sourceWithId(1)]);

    expect(searchMock).toHaveBeenCalledWith(1, 'one piece', { signal: expect.any(AbortSignal) });
    expect(outcomes).toEqual([
      {
        kind: 'withCandidates',
        source: sourceWithId(1),
        providerKey: 'suwayomi',
        candidates: [{ externalId: '731', title: 'Title 0', url: null, coverUrl: null }],
        discardedCandidatesCount: 0,
      },
    ]);
  });

  it('discards and counts the results without an external id', async () => {
    // Propósito: un resultado sin id del provider no puede convertirse en entry, así que no se ofrece, pero se
    // cuenta para poder avisar de que se descartó.
    //
    // Flujo: una fuente devuelve tres resultados, uno de ellos sin id. Salen dos candidatas y un descartado.
    searchMock.mockResolvedValue(listing('1', null, '3'));

    const [outcome] = await runSearch([sourceWithId(1)]);

    expect(outcome).toMatchObject({ kind: 'withCandidates', discardedCandidatesCount: 1 });
    expect(outcome.kind === 'withCandidates' && outcome.candidates.map((c) => c.externalId)).toEqual(['1', '3']);
  });

  it('reports a source as empty when it gives no candidates', async () => {
    searchMock.mockResolvedValueOnce(listing()).mockResolvedValueOnce(listing(null));

    const outcomes = await runSearch([sourceWithId(1), sourceWithId(2)]);

    expect(outcomes).toEqual([
      { kind: 'empty', source: sourceWithId(1), discardedCandidatesCount: 0 },
      { kind: 'empty', source: sourceWithId(2), discardedCandidatesCount: 1 },
    ]);
  });

  it('reports a source no provider serves as empty, not as failed', async () => {
    searchMock.mockRejectedValue(
      new ApiError(400, { type: ProblemType.READING_SOURCE_NOT_SUPPORTED, status: 400 }),
    );

    const outcomes = await runSearch([sourceWithId(1)]);

    expect(outcomes).toEqual([{ kind: 'empty', source: sourceWithId(1), discardedCandidatesCount: 0 }]);
  });

  it('reports a failing source as failed without affecting the others', async () => {
    searchMock.mockImplementation(async (sourceId) => {
      if (sourceId === 2) throw new ApiError(502, { type: ProblemType.READING_PROVIDER_FAILED, status: 502 });
      return listing('731');
    });

    const outcomes = await runSearch([sourceWithId(1), sourceWithId(2), sourceWithId(3)]);

    expect(outcomes.map((outcome) => [outcome.source.id, outcome.kind])).toEqual([
      [1, 'withCandidates'],
      [2, 'failed'],
      [3, 'withCandidates'],
    ]);
  });

  it('never searches in more sources at once than the limit', async () => {
    // Propósito: el tope de búsquedas simultáneas se respeta aunque haya más fuentes que huecos.
    //
    // Flujo: hay el doble de fuentes que el tope y cada búsqueda queda pendiente hasta que el test la libera. Se
    // anota cuántas hay a la vez; nunca supera el tope y todas acaban respondiendo.
    let inFlight = 0;
    let maxInFlight = 0;
    const releases: (() => void)[] = [];
    searchMock.mockImplementation(async () => {
      inFlight++;
      maxInFlight = Math.max(maxInFlight, inFlight);
      await new Promise<void>((resolve) => releases.push(resolve));
      inFlight--;
      return listing('731');
    });
    const sources = Array.from({ length: MAX_CONCURRENT_SOURCE_SEARCHES * 2 }, (_, index) => sourceWithId(index + 1));

    const run = runSearch(sources);
    while (releases.length < sources.length) {
      releases.forEach((release) => release());
      await Promise.resolve();
    }
    releases.forEach((release) => release());
    const outcomes = await run;

    expect(maxInFlight).toBe(MAX_CONCURRENT_SOURCE_SEARCHES);
    expect(outcomes).toHaveLength(sources.length);
  });

  it('reports nothing once the signal aborts, not even the answers already in flight', async () => {
    // Propósito: al lanzar otra búsqueda la anterior deja de entregar resultados, aunque algunas peticiones ya
    // estuvieran en curso.
    //
    // Flujo: se lanza la búsqueda en dos fuentes, se aborta con ambas pendientes y entonces responden. Ninguna se
    // reporta.
    const controller = new AbortController();
    const releases: (() => void)[] = [];
    searchMock.mockImplementation(async () => {
      await new Promise<void>((resolve) => releases.push(resolve));
      return listing('731');
    });

    const run = runSearch([sourceWithId(1), sourceWithId(2)], controller.signal);
    await Promise.resolve();
    controller.abort();
    releases.forEach((release) => release());
    const outcomes = await run;

    expect(outcomes).toEqual([]);
  });
});
