import { createContext } from 'react';

interface AuthContextType {
  accessToken: string | null;
  setAccessToken: (val: string | null) => void;
  refreshToken: () => Promise<number>;
}

export const AuthContext = createContext<AuthContextType>({
  accessToken: null,
  setAccessToken: () => {},
  refreshToken: async () => 0,
});
