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

export default function App() {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [modal, setModal] = useState(null as ModalContentProps | null);
  const authService = useMemo(() => new AuthService(), []); // useMemo -> do not re-create on re-render

  const logout = useCallback(() => {
    setAccessToken(null);
    authService.logoutUser();
    setModal({
      title: 'Logged out successfully',
      message: 'See you soon!',
      confirm: 'Go to home',
      type: 'info',
      redirect: '/',
    });
  }, [authService]);

  const refreshToken = useCallback(async (): Promise<number> => {
    const response = await authService.refreshToken();
    let responseCode = response.status;
    if ('accessToken' in response.data) {
      setAccessToken(response.data.accessToken);
    } else {
      const noSessionMessages = ['Missing refresh token in cookie', 'Refresh token not found'];
      const expiredSessionMessages = ['Refresh token is expired or revoked'];

      if (noSessionMessages.includes(response.data.details[0])) {
        responseCode = 400;
      } else if (expiredSessionMessages.includes(response.data.details[0])) {
        responseCode = 401;
      }
    }
    return responseCode;
  }, [authService]);

  const getModalPropsByStatus = useCallback((status: number): ModalContentProps | null => {
    if (status < 400) {
      return null;
    }
    // TODO: Frontend currently would treats all 401 from /api/auth/refresh endpoint as session expired.
    //       It will be refined once error codes are standardized. For a while, cases will be differentiated using response details.
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
    const run = async () => {
      const status = await refreshToken();
      setModal(getModalPropsByStatus(status));
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
  }, [getModalPropsByStatus, refreshToken]);

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
