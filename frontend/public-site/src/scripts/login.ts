// NOTE: could refactor with `register.ts` but isnt a priority,
// it will not visibly improve the performance
import AuthService, { type ErrorResponse } from '@common/services/AuthService';
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
    updateValidationErrors(
      validationErrorDiv,
      validationErrorList,
      errors.map((msg) => ({ code: 'VALIDATION_ERROR', title: msg })),
    );
    return;
  }

  const { email, password } = formData.data;

  const response = await authService.loginUser(email, password);
  if ('accessToken' in response.data) {
    validationErrorDiv.classList.add('invisible');
    successModal.showModal();
    successModal.addEventListener('close', () => {
      window.location.href = '/dashboard';
    });
    return;
  }

  const errorData = response.data as ErrorResponse;
  const errorDetails = errorData.errors?.length ? errorData.errors : [{ code: errorData.code, title: errorData.title }];
  updateValidationErrors(validationErrorDiv, validationErrorList, errorDetails);
});
