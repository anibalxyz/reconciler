import AuthService, { LoginResponse } from '@services/AuthService';

// TODO:
// - add validations with zod
// - add UI advices in both register & login pages

const authService: AuthService = new AuthService();

const loginForm = document.getElementById('loginForm') as HTMLFormElement;
loginForm.addEventListener('submit', async (event) => {
  event.preventDefault();

  const formData = new FormData(loginForm);
  const email = formData.get('email') as string;
  const password = formData.get('password') as string;

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
