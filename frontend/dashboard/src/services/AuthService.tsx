// This file MUST be refactored with /frontend/public-site/src/services/AuthService
interface AuthResponse {
  accessToken: string;
}

interface ErrorResponse {
  error: string;
  details: string[];
}
type RefreshResponse = AuthResponse | ErrorResponse;
type ApiResponse<T> = {
  status: number;
  data: T;
};

export default class AuthService {
  public async refreshToken(): Promise<ApiResponse<RefreshResponse>> {
    const res = await fetch('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    });

    return {
      status: res.status,
      data: await res.json(),
    };
  }
}
