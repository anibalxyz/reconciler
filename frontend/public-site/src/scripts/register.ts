import AuthService from '@common/services/AuthService';
import { registerSchema } from '@validation/authSchemas';
import { updateValidationErrors } from './ui';

const authService: AuthService = new AuthService();
const registerForm = document.getElementById('registerForm') as HTMLFormElement;
const validationErrorDiv = document.getElementById('validationErrors') as HTMLDivElement;
const validationErrorList = validationErrorDiv.querySelector('ul') as HTMLUListElement;
const successModal = document.getElementById('loginSuccessModal') as HTMLDialogElement;

registerForm.addEventListener('submit', async (event) => {
  event.preventDefault();

  const formData = registerSchema.safeParse(Object.fromEntries(new FormData(registerForm)));
  if (!formData.success) {
    const errors = Object.values(formData.error.flatten().fieldErrors).flatMap((msgs) => msgs ?? []);
    updateValidationErrors(validationErrorDiv, validationErrorList, errors);
    return;
  }

  const { name, email, password } = formData.data;

  const responseRegister = await authService.registerUser(name, email, password);
  if ('error' in responseRegister.data) {
    updateValidationErrors(validationErrorDiv, validationErrorList, responseRegister.data.details);
    return;
  }

  const responseLogin = await authService.loginUser(email, password);
  if ('error' in responseLogin.data) {
    updateValidationErrors(validationErrorDiv, validationErrorList, responseLogin.data.details);
    return;
  }

  validationErrorDiv.classList.add('invisible'); // after login -> smoother transition

  successModal.showModal();
  successModal.addEventListener('close', () => (window.location.href = '/dashboard'), { once: true });
});
