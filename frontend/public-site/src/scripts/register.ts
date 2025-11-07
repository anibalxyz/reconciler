import AuthService, { LoginResponse, RegistrationResponse } from '@services/AuthService';
import { registerSchema } from '@validation/authSchemas';

const authService: AuthService = new AuthService();

const registerForm = document.getElementById('registerForm') as HTMLFormElement;
registerForm.addEventListener('submit', async (event) => {
  event.preventDefault();

  const formData = registerSchema.safeParse(Object.fromEntries(new FormData(registerForm)));
  if (!formData.success) {
    console.log(formData.error.flatten().fieldErrors);
    return;
  }
  const { name, email, password } = formData.data;

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
