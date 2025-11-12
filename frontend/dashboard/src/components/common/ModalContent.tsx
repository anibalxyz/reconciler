import ErrorIcon from '@icons/ErrorIcon';
import InfoIcon from '@icons/InfoIcon';
import WarnIcon from '@icons/WarnIcon';

interface Props {
  title: string;
  message: string;
  type: 'info' | 'warn' | 'error';
}

const icons = {
  info: <InfoIcon key="info-icon" className="size-full" />,
  warn: <WarnIcon key="warn-icon" className="size-full" />,
  error: <ErrorIcon key="error-icon" className="size-full" />,
};

const styles = {
  info: 'bg-blue-600 hover:bg-blue-700 focus:ring-blue-400',
  warn: 'bg-yellow-600 hover:bg-yellow-700 focus:ring-yellow-400',
  error: 'bg-red-600 hover:bg-red-700 focus:ring-red-400',
};

export default function ModalContent({ title, message, type }: Props) {
  return (
    <div className="flex max-w-md flex-col bg-white p-6">
      <div className="flex flex-row justify-between max-md:flex-col max-md:items-center">
        <div className="w-full max-w-1/5">{icons[type]}</div>
        <div className="flex w-fit flex-col justify-center">
          <h1 id="modalTitle" className="w-fit text-xl font-semibold text-nowrap text-gray-900">
            {title}
          </h1>
          <p id="modalMessage" className="w-fit">
            {message}
          </p>
        </div>
      </div>
      <form method="dialog" className="mt-4 flex flex-row justify-center border-t p-4 pb-0">
        <button
          className={`rounded ${styles[type]} px-4 py-2 text-white transition focus:ring-2 focus:ring-offset-2 focus:outline-none`}
        >
          Login
        </button>
      </form>
    </div>
  );
}
