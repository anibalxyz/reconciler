import { ApiResponse, RefreshResponse } from '@common/services/AuthService';
import { createContext } from 'react';

interface AuthContextType {
  accessToken: string | null;
  refreshToken: () => Promise<ApiResponse<RefreshResponse> | null>;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextType>({
  accessToken: null,
  refreshToken: async () => null,
  logout: () => {},
});
