import AuthService, { LoginResponse, RegistrationResponse } from '@services/AuthService';
import { registerSchema } from '@validation/authSchemas';

const authService: AuthService = new AuthService();
const registerForm = document.getElementById('registerForm') as HTMLFormElement;
const validationErrorDiv = document.getElementById('validationErrors') as HTMLDivElement;
const validationErrorList = validationErrorDiv.querySelector('ul') as HTMLUListElement;
const successModal = document.getElementById('loginSuccessModal') as HTMLDialogElement;

function updateValidationErrors(errors: string[]) {
  validationErrorList.innerHTML = '';
  errors.forEach((error) => {
    const li = document.createElement('li');
    li.textContent = error;
    validationErrorList.appendChild(li);
  });
  validationErrorDiv.classList.remove('invisible');
}

registerForm.addEventListener('submit', async (event) => {
  event.preventDefault();

  const formData = registerSchema.safeParse(Object.fromEntries(new FormData(registerForm)));
  if (!formData.success) {
    const errors = Object.values(formData.error.flatten().fieldErrors).flatMap((msgs) => msgs ?? []);
    updateValidationErrors(errors);
    return;
  }

  const { name, email, password } = formData.data;

  // TODO: DRY
  const responseRegister: RegistrationResponse = await authService.registerUser(name, email, password);
  if ('error' in responseRegister) {
    updateValidationErrors(responseRegister.details);

    return;
  }

  const responseLogin: LoginResponse = await authService.loginUser(email, password);
  if ('error' in responseLogin) {
    updateValidationErrors(responseLogin.details);
    return;
  }

  validationErrorDiv.classList.add('invisible'); // after login -> smoother transition

  successModal.showModal();
  successModal.addEventListener('close', () => (window.location.href = '/dashboard'), { once: true });
});
