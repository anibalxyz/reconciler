import type { ErrorDetail } from '@common/services/AuthService';

function errorDetailToString(e: ErrorDetail): string {
  return e.field ? `${e.field}: ${e.title}` : e.title;
}

export function updateValidationErrors(
  errDiv: HTMLDivElement,
  errList: HTMLUListElement,
  errors: ErrorDetail[],
) {
  errList.innerHTML = '';
  errors.forEach((e) => {
    const li = document.createElement('li');
    li.textContent = errorDetailToString(e);
    errList.appendChild(li);
  });
  errDiv.classList.remove('invisible');
}
