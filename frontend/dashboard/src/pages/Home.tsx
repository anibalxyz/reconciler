import useAuth from '@hooks/useAuth';

export default function Home() {
  const { accessToken, logout } = useAuth();
  return (
    <main>
      <h1>This is the home</h1>
      <p>Context value: {accessToken}</p>
      <button onClick={logout}>Logout</button>
    </main>
  );
}
