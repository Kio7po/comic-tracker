import { ApiError, type ProblemDetailBody } from './ApiError';
import { getAccessToken, setAccessToken } from './authToken';

const baseURL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '/api';

// Forma del token de respuesta, que sigue oauth2.0
export interface TokenResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
}

// Un RequestInit (para el fetch) personalizado
// Permite recibir tanto un body: JSON.stringify(...) como un body: {...}
// De esta manera evitamos tener que llamar a JSON.stringify desde donde se invoque el fetch
export interface ApiRequestInit extends Omit<RequestInit, 'body'> {
  body?: BodyInit | object;
}

// Permite distinguir si el body ya es un tipo válido y no debe convertirse en json
// Necesario ya que admitimos recibir un body {...} sin pasar por stringify
function isBodyInit(value: unknown): value is BodyInit {
  return (
    typeof value === 'string' ||
    value instanceof FormData ||
    value instanceof Blob ||
    value instanceof URLSearchParams ||
    value instanceof ArrayBuffer ||
    value instanceof ReadableStream
  );
}

// Intenta obtener el cuerpo del error y si falla devuelve null
async function parseProblemDetail(res: Response): Promise<ProblemDetailBody | null> {
  return (await res.json().catch(() => null)) as ProblemDetailBody | null;
}

// Concurrent 401s share one in-flight refresh instead of each firing their own.
let refreshPromise: Promise<string> | null = null;

function refreshAccessToken(): Promise<string> {
  // Si no existe, se hace el fetch, pero si ya existe se obtiene la Promise existente y salta al return
  refreshPromise ??= fetch(`${baseURL}/auth/refresh`, { method: 'POST', credentials: 'include' })
    .then(async (res) => {
      if (!res.ok) {
        throw new ApiError(res.status, await parseProblemDetail(res));
      }
      const body = (await res.json()) as TokenResponse;
      setAccessToken(body.access_token);
      return body.access_token;
    })
    .finally(() => {
      refreshPromise = null;
    });
  return refreshPromise;
}

async function rawFetch(path: string, init: ApiRequestInit, retried: boolean): Promise<Response> {
  const headers = new Headers(init.headers);
  const token = getAccessToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  let body: BodyInit | undefined;
  // Debido a que permitimos que el body sea un objeto, hay que revisar si es ya un body válido
  // Si no lo es, es un objeto/array y debe convertirse a JSON.
  if (init.body !== undefined) {
    if (isBodyInit(init.body)) {
      body = init.body;
    } else {
      body = JSON.stringify(init.body);
      headers.set('Content-Type', 'application/json');
    }
  }

  const res = await fetch(`${baseURL}${path}`, { ...init, headers, body, credentials: 'include' });

  // Si falla con un 401 y no es un reintento ni estamos llamando a /refresh
  // tenemos que obtener un token nuevo y reintentar la llamada con él.
  // Pero solo únicamente si ese 401 viene del propio filtro de seguridad rechazando el JWT,
  // no por un 401 causado por una regla de negocio. Se distingue porque, a diferencia de un
  // 401 de negocio (p. ej. credenciales de login incorrectas), no trae cuerpo ProblemDetail.
  // Reintentar en el caso de un 401 de negocio sustituiría el error real por el
  // 401 del propio /refresh, perdiendo el motivo original.
  if (res.status === 401 && !retried && !path.startsWith('/auth/refresh')) {
    const problem = await parseProblemDetail(res);
    if (problem === null) {
      try {
        await refreshAccessToken();
      } catch (refreshError) {
        setAccessToken(null);
        throw refreshError;
      }
      return rawFetch(path, init, true);
    } else {
      throw new ApiError(res.status, problem);
    }
  }

  if (!res.ok) {
    throw new ApiError(res.status, await parseProblemDetail(res));
  }

  return res;
}

export async function apiFetch<T = void>(path: string, init: ApiRequestInit = {}): Promise<T> {
  const res = await rawFetch(path, init, false);
  if (res.status === 204) {
    return undefined as T; // si ocurre T debería ser void
  }
  return (await res.json()) as T;
}
