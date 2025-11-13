export function updateValidationErrors(errDiv: HTMLDivElement, errList: HTMLUListElement, errors: string[]) {
  errList.innerHTML = '';
  errors.forEach((error) => {
    const li = document.createElement('li');
    li.textContent = error;
    errList.appendChild(li);
  });
  errDiv.classList.remove('invisible');
}
