import { describe, expect, it } from 'vitest';
import { runWithConcurrencyLimit } from './runWithConcurrencyLimit';

// Una tarea que no termina hasta que el test la libera con `resolve()`.
function deferred() {
  let resolve!: () => void;
  const promise = new Promise<void>((r) => {
    resolve = r;
  });
  return { promise, resolve };
}

describe('runWithConcurrencyLimit', () => {
  it('runs every item', async () => {
    const seen: number[] = [];

    await runWithConcurrencyLimit([1, 2, 3, 4, 5], 2, async (item) => {
      seen.push(item);
    });

    expect(seen.toSorted()).toEqual([1, 2, 3, 4, 5]);
  });

  it('never has more than the limit in flight and starts items in order', async () => {
    // Propósito: el tope se respeta y, al liberarse un hueco, arranca el siguiente item de la lista.
    //
    // Flujo:
    // 1. Hay cinco items, un tope de dos y cada tarea espera a que el test la libere.
    // 2. Solo arrancan los dos primeros (0 y 1).
    // 3. Al terminar el 0 arranca el 2, y al terminar el 1 arranca el 3.
    // 4. Al liberar el resto arranca el 4 y todo termina.
    const gates = Array.from({ length: 5 }, deferred);
    const started: number[] = [];

    const run = runWithConcurrencyLimit([0, 1, 2, 3, 4], 2, async (index) => {
      started.push(index);
      await gates[index].promise;
    });

    await Promise.resolve();
    expect(started).toEqual([0, 1]);

    gates[0].resolve();
    await Promise.resolve();
    await Promise.resolve();
    expect(started).toEqual([0, 1, 2]);

    gates[1].resolve();
    await Promise.resolve();
    await Promise.resolve();
    expect(started).toEqual([0, 1, 2, 3]);

    gates.forEach((gate) => gate.resolve());
    await run;
    expect(started).toEqual([0, 1, 2, 3, 4]);
  });

  it('keeps running the rest when one task fails', async () => {
    const seen: number[] = [];

    await runWithConcurrencyLimit([1, 2, 3], 1, async (item) => {
      seen.push(item);
      if (item === 2) throw new Error('boom');
    });

    expect(seen).toEqual([1, 2, 3]);
  });

  it('stops starting new items once the signal aborts', async () => {
    // Propósito: tras abortar la señal no arranca ningún item más.
    //
    // Flujo: con tope de uno, la tarea del item 2 aborta la señal; los items 3 y 4 ya no llegan a arrancar.
    const controller = new AbortController();
    const seen: number[] = [];

    await runWithConcurrencyLimit(
      [1, 2, 3, 4],
      1,
      async (item) => {
        seen.push(item);
        if (item === 2) controller.abort();
      },
      controller.signal,
    );

    expect(seen).toEqual([1, 2]);
  });

  it('starts nothing when the signal is already aborted', async () => {
    const controller = new AbortController();
    controller.abort();
    const seen: number[] = [];

    await runWithConcurrencyLimit(
      [1, 2],
      2,
      async (item) => {
        seen.push(item);
      },
      controller.signal,
    );

    expect(seen).toEqual([]);
  });

  it('resolves immediately with no items', async () => {
    await expect(runWithConcurrencyLimit([], 6, async () => undefined)).resolves.toBeUndefined();
  });
});
