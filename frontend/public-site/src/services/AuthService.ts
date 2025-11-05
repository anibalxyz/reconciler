interface UserCreateResponse {
  id: number;
  name: string;
  email: string;
}

interface AuthResponse {
  accessToken: string;
}

interface ErrorResponse {
  error: string;
  details: string[];
}

type RegistrationResponse = UserCreateResponse | ErrorResponse;
type LoginResponse = AuthResponse | ErrorResponse;

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

    return fetch('auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    }).then((res) => res.json());
  }
}
