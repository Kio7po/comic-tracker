import { createContext, useContext, useLayoutEffect, useMemo, type ReactNode } from 'react';
import { useLocalStorage } from '@/common/hooks/useLocalStorage';

export type Theme = 'light' | 'dark' | 'system';

interface ThemeContextValue {
  theme: Theme;
  setTheme: (theme: Theme) => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

const THEME_STORAGE_KEY = 'theme';
const DARK_MEDIA_QUERY = '(prefers-color-scheme: dark)';

function isDark(theme: Theme): boolean {
  return theme === 'dark' || (theme === 'system' && window.matchMedia(DARK_MEDIA_QUERY).matches);
}

function ThemeProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [theme, setTheme] = useLocalStorage<Theme>(THEME_STORAGE_KEY, 'system');

  // useLayoutEffect (not useEffect) so the class lands before the browser's first paint,
  // avoiding a flash of the wrong theme on load.
  useLayoutEffect(() => {
    document.documentElement.classList.toggle('dark', isDark(theme));

    if (theme !== 'system') {
      return;
    }
    // "system" needs to keep following the OS preference live while the app stays open
    const mediaQueryList = window.matchMedia(DARK_MEDIA_QUERY);
    const listener = () => document.documentElement.classList.toggle('dark', isDark(theme));
    mediaQueryList.addEventListener('change', listener);
    return () => mediaQueryList.removeEventListener('change', listener);
  }, [theme]);

  // useMemo para crear una referencia estable al objeto que solo cambie si
  // realmente cambian sus valores. Evitamos que el objeto cambie en cada render
  // y cause actualizaciones innecesarias en los consumidores.
  const value = useMemo(() => ({ theme, setTheme }), [theme, setTheme]);

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

function useTheme(): ThemeContextValue {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
}

export { ThemeProvider, useTheme };
