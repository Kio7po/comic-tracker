import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { login as apiLogin, logout as apiLogout } from '@/services/user/api/auth';
import { me } from '@/services/user/api/user';
import type { LoginRequest, UserResponse } from '@/services/user/types';

interface AuthContextValue {
  user: UserResponse | null;
  isLoading: boolean;
  login: (request: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  updateUser: (user: UserResponse) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function AuthProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // apiFetch retries this against /auth/refresh on a token-less 401, so this also restores
    // the session after a page reload when the user had a persistent ("remember me") cookie.
    me()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setIsLoading(false));
  }, []);

  // useCallback para generar una referencia estable a los métodos y de esa forma
  // poder añadirlos como dependencia al useMemo de forma segura.
  // Podrían no añadirse como dependencia al useMemo ya que son funciones estables,
  // pero rompería la regla de react que dice que las dependencias deben incluir
  // cualquier valor asignado dentro del componente.
  const login = useCallback(async (request: LoginRequest) => {
    await apiLogin(request);
    setUser(await me());
  }, []);

  const logout = useCallback(async () => {
    await apiLogout();
    setUser(null);
  }, []);

  // setUser ya es una referencia estable (la garantiza useState), así que updateUser no
  // necesita su propio useCallback - se reexpone directamente con el nombre que espera el
  // contexto. A diferencia de login/logout, quien llama a updateUser (p. ej. la pantalla de
  // ajustes) hace su propia llamada a la API y solo usa esto para sincronizar el resultado.
  const updateUser = setUser;

  // useMemo para crear una referencia estable al objeto que solo cambie si
  // realmente cambian sus valores. De esta manera evitamos que el objeto cambie
  // en cada render y cause actualizaciones innecesarias en los consumers.
  const value = useMemo(
    () => ({user, isLoading, login, logout, updateUser}),
    [user, isLoading, login, logout, updateUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export { AuthProvider, useAuth };
