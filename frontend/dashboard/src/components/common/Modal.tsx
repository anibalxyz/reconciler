import { useEffect, useRef } from 'react';

interface Props {
  onClose: () => void;
  children: React.ReactElement;
}

// I'm not convinced of this but isnt a priority now
export default function Modal({ onClose, children }: Props) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    ref.current?.showModal(); // Used to show ::backdrop
  }, []);

  return (
    <dialog
      ref={ref}
      className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 rounded-lg shadow-lg backdrop:bg-black/50"
      aria-modal="true"
      aria-labelledby="modalTitle"
      aria-describedby="modalMessage"
      role="alertdialog"
      onClose={onClose}
    >
      {children}
    </dialog>
  );
}
