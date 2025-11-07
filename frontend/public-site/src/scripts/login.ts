import AuthService, { LoginResponse } from '@services/AuthService';
import { loginSchema } from '@validation/authSchemas';

// TODO:
// - add UI advices in both register & login pages

const authService: AuthService = new AuthService();

const loginForm = document.getElementById('loginForm') as HTMLFormElement;
loginForm.addEventListener('submit', async (event) => {
  event.preventDefault();

  const formData = loginSchema.safeParse(Object.fromEntries(new FormData(loginForm)));
  if (!formData.success) {
    console.log(formData.error.flatten().fieldErrors);
    return;
  }
  const { email, password } = formData.data;

  const response: LoginResponse = await authService.loginUser(email, password);
  if ('error' in response) {
    console.log(`Login failed: ${response.error}\nDetails: ${response.details.join(', ')}`);
    return;
  }

  const successModal = document.getElementById('loginSuccessModal') as HTMLDialogElement;
  successModal.showModal();
  successModal.addEventListener('close', () => {
    window.location.href = '/dashboard';
  });
});
