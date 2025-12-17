import useAuth from '@hooks/useAuth';

export default function Home() {
  const { logout } = useAuth();

  return (
    // NOTE: yes, AI-generated :)
    <main className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <section className="w-full max-w-md rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <h1 className="text-xl font-semibold text-gray-900">Dashboard (WIP)</h1>

        <p className="mt-2 text-sm text-gray-600">
          This dashboard is still under development. For now, you can interact with the API via{' '}
          <a href="/swagger" className="font-medium text-blue-600 hover:underline">
            Swagger UI
          </a>
          .
        </p>

        <div className="mt-6 flex items-center justify-between">
          <span className="text-xs text-gray-400">Temporary view</span>

          <button
            onClick={logout}
            className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 focus:ring-2 focus:ring-red-500 focus:outline-none"
          >
            Logout
          </button>
        </div>
      </section>
    </main>
  );
}
