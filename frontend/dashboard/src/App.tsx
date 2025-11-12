import Home from '@pages/Home';
import { AuthContext } from '@context/AuthContext';
import { useCallback, useEffect, useMemo, useState } from 'react';
import AuthService from '@common/services/AuthService';
import Modal from '@components/common/Modal';
import ModalContent from '@components/common/ModalContent';

interface Warning {
  title: string;
  message: string;
  icon: 'info' | 'warn' | 'error';
}

// TODO: review -> seems too overloaded
export default function App() {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [warning, setWarning] = useState(null as Warning | null);
  const authService = useMemo(() => new AuthService(), []); // useMemo -> do not re-create on re-render

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
            setWarning({
              title: "It looks like you're not logged in",
              message: 'Please sign in to continue',
              icon: 'info',
            });
            break;
          case 401:
            setWarning({
              title: 'Your session has expired',
              message: 'Please sign in again',
              icon: 'info',
            });
            break;
          default:
            setWarning({
              title: 'An unknown error occurred',
              message: 'Please try again later',
              icon: 'warn',
            });
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

  if (warning)
    return (
      <Modal onClose={() => (window.location.href = '/login')}>
        <ModalContent message={warning.message} title={warning.title} type={warning.icon}></ModalContent>
      </Modal>
    );

  return (
    <AuthContext value={{ accessToken, setAccessToken, refreshToken }}>
      <Home />
    </AuthContext>
  );
}
