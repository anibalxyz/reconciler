import { createContext } from 'react';

interface AuthContextType {
  accessToken: string | null;
  refreshToken: () => Promise<number>;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextType>({
  accessToken: null,
  refreshToken: async () => 0,
  logout: () => {},
});
