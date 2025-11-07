import { useEffect, useState } from 'react';

type ApiStatus = 'loading' | 'healthy' | 'unhealthy' | 'error';
const STATUS_MESSAGES = {
  loading: 'Connecting...',
  healthy: 'API is healthy (able to access the database)',
  unhealthy: 'API is unhealthy (unable to access the database)',
  error: 'Unable to connect to API :(',
};

async function fetchStatus(
  abortSignal: AbortSignal,
  setStatus: React.Dispatch<React.SetStateAction<ApiStatus>>,
): Promise<void> {
  try {
    const response = await fetch('/api/health', {
      signal: abortSignal,
    });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const data: { dbIsConnected: boolean } = await response.json();
    console.log('Health check response:', data);
    const status: ApiStatus = data.dbIsConnected ? 'healthy' : 'unhealthy';

    setStatus(status);
  } catch (err: Error | unknown) {
    if (!abortSignal.aborted) {
      console.error('ERROR: ' + err);
      setStatus('error');
    }
  }
}

export default function App() {
  const [status, setStatus] = useState<ApiStatus>('loading');

  useEffect(() => {
    const abortController = new AbortController();
    const abortSignal = abortController.signal;

    fetchStatus(abortSignal, setStatus);

    return () => {
      abortController.abort();
    };
  }, []);
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-gray-50 p-6 font-sans text-gray-800">
      <p className="mb-4 text-sm">
        Go Home:{' '}
        <a href="/" className="text-blue-600 hover:underline">
          /
        </a>
      </p>
      <h1 className="mb-4 text-2xl font-semibold">API Health Check</h1>
      <div className="flex items-center gap-2 rounded bg-white p-4 shadow">
        <span className="font-medium">Status:</span>
        <span
          className={`font-semibold ${
            status === 'healthy'
              ? 'text-green-600'
              : status === 'unhealthy'
                ? 'text-yellow-600'
                : status === 'error'
                  ? 'text-red-600'
                  : 'text-gray-500'
          }`}
        >
          {STATUS_MESSAGES[status]}
        </span>
      </div>
    </main>
  );
}
