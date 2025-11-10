import Home from './pages/Home';
import { AuthContext } from './context/AuthContext';
import { useEffect, useState } from 'react';
import AuthService from './services/AuthService';
import UnauthorizedModal from './components/UnauthorizedModal';

const authService: AuthService = new AuthService();

export default function App() {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState({
    title: '',
    message: '',
  });
  const success: boolean = message.title.length === 0;

  async function refreshToken() {
    const response = await authService.refreshToken();
    if ('error' in response.data) {
      console.error(response.data.error);
      switch (response.status) {
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
    } else {
      setAccessToken(response.data.accessToken);
    }
  }

  useEffect(() => {
    const run = async () => {
      await refreshToken();
      setLoading(false);
    };
    run();
  }, []);

  if (loading) {
    // TODO: add loading/spinner component, specially if it takes too long
    return null;
  }

  if (!success) {
    return <UnauthorizedModal message={message} />;
  }

  return (
    <AuthContext value={{ accessToken, setAccessToken }}>
      <Home />
    </AuthContext>
  );
}
