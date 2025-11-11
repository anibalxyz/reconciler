import AuthService from '@common/services/AuthService';
import { loginSchema } from '@validation/authSchemas';

const authService: AuthService = new AuthService();
const loginForm = document.getElementById('loginForm') as HTMLFormElement;
const validationErrorDiv = document.getElementById('validationErrors') as HTMLDivElement;
const validationErrorList = validationErrorDiv.querySelector('ul') as HTMLUListElement;
const successModal = document.getElementById('loginSuccessModal') as HTMLDialogElement;

// TODO: DRY
function updateValidationErrors(errors: string[]) {
  validationErrorList.innerHTML = '';
  errors.forEach((error) => {
    const li = document.createElement('li');
    li.textContent = error;
    validationErrorList.appendChild(li);
  });
  validationErrorDiv.classList.remove('invisible');
}

loginForm.addEventListener('submit', async (event) => {
  event.preventDefault();

  const formData = loginSchema.safeParse(Object.fromEntries(new FormData(loginForm)));
  if (!formData.success) {
    const errors = Object.values(formData.error.flatten().fieldErrors).flatMap((msgs) => msgs ?? []);
    updateValidationErrors(errors);
    return;
  }

  const { email, password } = formData.data;

  const response = await authService.loginUser(email, password);
  if ('error' in response.data) {
    updateValidationErrors(response.data.details);
    return;
  }

  validationErrorDiv.classList.add('invisible'); // after login -> smoother transition

  successModal.showModal();
  successModal.addEventListener('close', () => {
    window.location.href = '/dashboard';
  });
});
