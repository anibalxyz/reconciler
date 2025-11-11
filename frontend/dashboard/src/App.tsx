import Home from './pages/Home';
import { AuthContext } from './context/AuthContext';
import { useCallback, useEffect, useMemo, useState } from 'react';
import AuthService from '@common/services/AuthService';
import UnauthorizedModal from './components/UnauthorizedModal';

export default function App() {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState({
    title: '',
    message: '',
  });
  const authService = useMemo(() => new AuthService(), []); // useMemo -> do not re-create

  const refreshToken = useCallback(async (): Promise<number> => {
    const response = await authService.refreshToken();
    if ('accessToken' in response.data) {
      setAccessToken(response.data.accessToken);
    }
    return response.status;
  }, [authService]);

  useEffect(() => {
    const run = async () => {
      const status = await refreshToken();
      if (status >= 400) {
        switch (status) {
          case 400:
            setMessage({
              title: "It looks like you're not logged in",
              message: 'Please sign in to continue',
            });
            break;
          case 401:
            setMessage({
              title: 'Your session has expired',
              message: 'Please sign in again',
            });
            break;
          default:
            // TODO: handle unknown errors globally (shared modal)
            console.log('Unknown error');
            break;
        }
      }
      setLoading(false);
    };

    // This waits until the browser finishes reloading and all cookies are reattached
    // before executing refreshToken(), preventing invalid session errors on fast reloads (F5 spam)
    if (document.readyState === 'complete') {
      run();
    } else {
      window.addEventListener('load', run);
    }
    return () => {
      window.removeEventListener('load', run);
    };
  }, [refreshToken]);

  if (loading) return <div>Loading...</div>;

  if (message.title.length > 0) return <UnauthorizedModal message={message} />;

  return (
    <AuthContext value={{ accessToken, setAccessToken, refreshToken }}>
      <Home />
    </AuthContext>
  );
}
