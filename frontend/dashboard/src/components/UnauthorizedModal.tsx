import { useEffect, useRef } from 'react';

interface Props {
  message: {
    title: string;
    message: string;
  };
}

// TODO: when adding more modals, generalize this one
export default function UnauthorizedModal({ message }: Props) {
  const ref = useRef<HTMLDialogElement>(null);

  function goToLogin() {
    window.location.href = '/login';
  }

  useEffect(() => {
    ref.current?.showModal(); // Used to show ::backdrop
  }, []);

  return (
    <dialog
      ref={ref}
      id="dada"
      className="fixed top-1/2 left-1/2 max-w-lg -translate-x-1/2 -translate-y-1/2 rounded-lg bg-white p-6 text-center shadow-lg backdrop:bg-black/50"
      aria-modal="true"
      aria-labelledby="modalTitle"
      aria-describedby="modalMessage"
      role="alertdialog"
    >
      <h1 id="modalTitle" className="text-lg font-semibold text-gray-900">
        {message.title}
      </h1>
      <p id="modalMessage">{message.message}</p>
      <form method="dialog" className="mt-4">
        <button onClick={goToLogin} className="rounded bg-blue-600 px-4 py-2 text-white transition hover:bg-blue-700">
          Login
        </button>
      </form>
    </dialog>
  );
}
