import AuthService, { type ErrorResponse } from '@common/services/AuthService';
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
    updateValidationErrors(
      validationErrorDiv,
      validationErrorList,
      errors.map((msg) => ({ code: 'VALIDATION_ERROR', title: msg })),
    );
    return;
  }

  const { name, email, password } = formData.data;

  const responseRegister = await authService.registerUser(name, email, password);
  if ('id' in responseRegister.data) {
    // registration succeeded, now login
    const responseLogin = await authService.loginUser(email, password);
    if ('accessToken' in responseLogin.data) {
      validationErrorDiv.classList.add('invisible');
      successModal.showModal();
      successModal.addEventListener('close', () => (window.location.href = '/dashboard'), { once: true });
      return;
    }

    const loginError = responseLogin.data as ErrorResponse;
    const loginDetails = loginError.errors?.length ? loginError.errors : [{ code: loginError.code, title: loginError.title }];
    updateValidationErrors(validationErrorDiv, validationErrorList, loginDetails);
    return;
  }

  const registerError = responseRegister.data as ErrorResponse;
  const registerDetails = registerError.errors?.length ? registerError.errors : [{ code: registerError.code, title: registerError.title }];
  updateValidationErrors(validationErrorDiv, validationErrorList, registerDetails);
});
