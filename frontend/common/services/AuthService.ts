export interface AuthResponse {
  accessToken: string;
}

// TODO: this is NOT the real backend ErrorDetail structure.
//       The real record only guarantees `code` (String).
//       `title` and `field` are assumptions based on current validation errors
export interface ErrorDetail {
  code: string;
  title: string;
  field?: string;
}

export interface ErrorResponse {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  code: string;
  errors?: ErrorDetail[];
}

export interface UserCreateResponse {
  id: number;
  name: string;
  email: string;
}

export type RegistrationResponse = UserCreateResponse | ErrorResponse;
export type LoginResponse = AuthResponse | ErrorResponse;
export type LogoutResponse = void | ErrorResponse;
export type RefreshResponse = AuthResponse | ErrorResponse;

export type ApiResponse<T> = {
  status: number;
  data: T;
};

const serverUnreachableError: ErrorResponse = {
  title: 'Server unreachable',
  detail: 'Looks like we are having some problems :( Please try again later!',
  type: '',
  status: 500,
  code: 'UNREACHABLE_ERROR',
};

async function performRequest<T>(request: Request): Promise<ApiResponse<T | ErrorResponse>> {
  try {
    const res = await fetch(request);

    let data: T | ErrorResponse;
    if (res.status === 204) {
      data = undefined as T;
    } else {
      data = await res.json();
    }

    return { status: res.status, data };
  } catch {
    return {
      status: 503,
      data: serverUnreachableError,
    };
  }
}

export default class AuthService {
  public async refreshToken(): Promise<ApiResponse<RefreshResponse>> {
    const request = new Request('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    });
    return performRequest(request);
  }

  public async registerUser(name: string, email: string, password: string): Promise<ApiResponse<RegistrationResponse>> {
    const payload = { name, email, password };
    const request = new Request('/api/users', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });
    return performRequest(request);
  }

  public async loginUser(email: string, password: string): Promise<ApiResponse<LoginResponse>> {
    const payload = { email, password };
    const request = new Request('/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
      credentials: 'include',
    });
    return performRequest(request);
  }

  public async logoutUser(): Promise<ApiResponse<LogoutResponse>> {
    const request = new Request('/api/auth/logout', {
      method: 'POST',
      credentials: 'include',
    });
    return performRequest(request);
  }
}
