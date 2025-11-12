import { AuthContext } from '@context/AuthContext';
import { useCallback, useContext } from 'react';

export default function useAuth() {
  const { accessToken, setAccessToken, refreshToken } = useContext(AuthContext);

  const login = useCallback(async () => {
    refreshToken();
  }, [refreshToken]);

  const logout = useCallback(() => {
    setAccessToken(null);
    // TODO: add and call backend logout endpoint to invalidate refresh token and clear cookie
    window.location.href = '/login';
  }, [setAccessToken]);

  return { accessToken, login, logout };
}
