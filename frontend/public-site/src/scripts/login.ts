// NOTE: could refactor with `register.ts` but isnt a priority,
// it will not visibly improve the performance
import AuthService from '@common/services/AuthService';
import { loginSchema } from '@validation/authSchemas';
import { updateValidationErrors } from './ui';

const authService: AuthService = new AuthService();
const loginForm = document.getElementById('loginForm') as HTMLFormElement;
const validationErrorDiv = document.getElementById('validationErrors') as HTMLDivElement;
const validationErrorList = validationErrorDiv.querySelector('ul') as HTMLUListElement;
const successModal = document.getElementById('loginSuccessModal') as HTMLDialogElement;

loginForm.addEventListener('submit', async (event) => {
  event.preventDefault();

  const formData = loginSchema.safeParse(Object.fromEntries(new FormData(loginForm)));
  if (!formData.success) {
    const errors = Object.values(formData.error.flatten().fieldErrors).flatMap((msgs) => msgs ?? []);
    updateValidationErrors(validationErrorDiv, validationErrorList, errors);
    return;
  }

  const { email, password } = formData.data;

  const response = await authService.loginUser(email, password);
  if ('error' in response.data) {
    updateValidationErrors(validationErrorDiv, validationErrorList, response.data.details);
    return;
  }

  validationErrorDiv.classList.add('invisible'); // after login -> smoother transition

  successModal.showModal();
  successModal.addEventListener('close', () => {
    window.location.href = '/dashboard';
  });
});
