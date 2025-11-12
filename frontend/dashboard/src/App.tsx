import Home from '@pages/Home';
import { AuthContext } from '@context/AuthContext';
import { useCallback, useEffect, useMemo, useState } from 'react';
import AuthService from '@common/services/AuthService';
import Modal from '@components/common/Modal';
import ModalContent from '@components/common/ModalContent';
import { Props as ModalProps } from '@components/common/ModalContent';

type ModalContentProps = ModalProps & {
  redirect: string;
};

// TODO: review -> seems too overloaded
export default function App() {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [modal, setModal] = useState(null as ModalContentProps | null);
  const authService = useMemo(() => new AuthService(), []); // useMemo -> do not re-create on re-render

  const logout = useCallback(() => {
    setAccessToken(null);
    // TODO: add authService.logout();
    setModal({
      title: 'Logged out successfully',
      message: 'See you soon!',
      confirm: 'Go to home',
      type: 'info',
      redirect: '/',
    });
  }, []);

  const refreshToken = useCallback(async (): Promise<number> => {
    const response = await authService.refreshToken();
    if ('accessToken' in response.data) {
      setAccessToken(response.data.accessToken);
    }
    return response.status;
  }, [authService]);

  const getModalPropsByStatus = useCallback((status: number): ModalContentProps | null => {
    if (status < 400) {
      return null;
    }
    switch (status) {
      case 400:
        return {
          title: "It looks like you're not logged in",
          message: 'Please sign in to continue',
          confirm: 'Sign in',
          type: 'warn',
          redirect: '/login',
        };
      case 401:
        return {
          title: 'Your session has expired',
          message: 'Please sign in again',
          confirm: 'Sign in',
          type: 'info',
          redirect: '/login',
        };
      default:
        return {
          title: 'An unknown error occurred',
          message: 'Please try again later',
          confirm: 'Go to home',
          type: 'error',
          redirect: '/',
        };
    }
  }, []);

  useEffect(() => {
    if (accessToken !== null) return; // just executed at startup and logout
    const run = async () => {
      // modal already setted = logout was executed, then execute startup stuff
      if (modal === null) {
        const status = await refreshToken();
        setModal(getModalPropsByStatus(status));
      }
    };

    // This waits until the browser finishes reloading and all cookies are reattached
    // before executing refreshToken(), preventing invalid session errors on fast reloads (F5 spam)
    const documentIsReady = document.readyState === 'complete';
    if (documentIsReady) {
      run();
    } else {
      window.addEventListener('load', run);
    }
    return () => {
      if (!documentIsReady) window.removeEventListener('load', run);
    };
  }, [logout, getModalPropsByStatus, refreshToken, accessToken, modal]);

  if (modal)
    return (
      <Modal onClose={() => (window.location.href = modal.redirect)}>
        <ModalContent {...modal}></ModalContent>
      </Modal>
    );

  if (accessToken)
    return (
      <AuthContext value={{ accessToken, refreshToken, logout }}>
        <Home />
      </AuthContext>
    );

  return <div>Loading...</div>;
}
