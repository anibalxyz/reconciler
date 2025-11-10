import Home from './pages/Home';
import { AuthContext } from './context/AuthContext';
import { useEffect, useState } from 'react';
import AuthService from './services/AuthService';

const authService: AuthService = new AuthService();

export default function App() {
  const [accessToken, setAccessToken] = useState<string | null>(null);

  async function refreshToken(): Promise<string | null> {
    const response = await authService.refreshToken();
    // TODO: show modal with redirection | access the dashboard "Home"
    if ('error' in response.data) {
      console.error(response.data.error);
      switch (response.status) {
        case 400:
          console.log("It looks like you're not logged in. Please sign in to continue");
          break;
        case 401:
          console.log('Your session has expired. Please sign in again');
          break;
        default:
          // TODO: handle unknown errors globally (shared modal)
          console.log('Unknown error');
          break;
      }
      return null;
    } else {
      return response.data.accessToken;
    }
  }

  useEffect(() => {
    const run = async () => {
      const token = await refreshToken();
      setAccessToken(token);
    };
    run();
  }, []);
  return (
    <AuthContext value={{ accessToken, setAccessToken }}>
      <Home />
    </AuthContext>
  );
}
