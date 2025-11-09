interface UserCreateResponse {
  id: number;
  name: string;
  email: string;
}

// TODO: remove this return in API
interface AuthResponse {
  accessToken: string;
}

interface ErrorResponse {
  error: string;
  details: string[];
}

export type RegistrationResponse = UserCreateResponse | ErrorResponse;
export type LoginResponse = AuthResponse | ErrorResponse;

export default class AuthService {
  public async registerUser(name: string, email: string, password: string): Promise<RegistrationResponse> {
    const payload = { name, email, password };

    return fetch('/api/users', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    }).then((res) => res.json());
  }

  public async loginUser(email: string, password: string): Promise<LoginResponse> {
    const payload = { email, password };

    return fetch('/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
      credentials: 'include',
    }).then((res) => res.json());
  }
}
