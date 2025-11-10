import { AuthContext } from '@/context/AuthContext';
import { useContext } from 'react';

export default function Home() {
  const { accessToken } = useContext(AuthContext);
  return (
    <main>
      <h1>This is the home</h1>
      <p>Context value: {accessToken}</p>
    </main>
  );
}
