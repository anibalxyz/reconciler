import AuthService, { LoginResponse, RegistrationResponse } from '@services/AuthService';

const authService: AuthService = new AuthService();

const registerForm = document.getElementById('registerForm') as HTMLFormElement;
registerForm.addEventListener('submit', async (event) => {
  event.preventDefault();

  const formData = new FormData(registerForm);
  const name = formData.get('name') as string;
  const email = formData.get('email') as string;
  const password = formData.get('password') as string;
  const passwordConfirm = formData.get('passwordConfirm') as string;

  if (password !== passwordConfirm) {
    console.log('Passwords do not match');
    return;
  }

  const responseRegister: RegistrationResponse = await authService.registerUser(name, email, password);
  if ('error' in responseRegister) {
    console.log(`Registration failed: ${responseRegister.error}\nDetails: ${responseRegister.details.join(', ')}`);
    return;
  }

  const responseLogin: LoginResponse = await authService.loginUser(email, password);
  if ('error' in responseLogin) {
    console.log(`Login failed: ${responseLogin.error}\nDetails: ${responseLogin.details.join(', ')}`);
    return;
  }

  const successModal = document.getElementById('loginSuccessModal') as HTMLDialogElement;
  successModal.showModal();
  successModal.addEventListener('close', () => (window.location.href = '/dashboard'), { once: true });
});
