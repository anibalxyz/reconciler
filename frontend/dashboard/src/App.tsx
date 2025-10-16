import "./App.css";
import { useEffect, useState } from "react";

type ApiStatus = "loading" | "healthy" | "unhealthy" | "error";
const STATUS_MESSAGES = {
  loading: "Connecting...",
  healthy: "API is healthy (able to access the database)",
  unhealthy: "API is unhealthy (unable to access the database)",
  error: "Unable to connect to API :(",
};

export default function App() {
  const [status, setStatus] = useState<ApiStatus>("loading");

  useEffect(() => {
    const abortController = new AbortController();
    const abortSignal = abortController.signal;

    async function fetchStatus(): Promise<void> {
      try {
        const response = await fetch("/api/health", {
          signal: abortSignal,
        });
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }
        const data: { dbIsConnected: boolean } = await response.json();
        console.log("Health check response:", data);
        const status: ApiStatus = data.dbIsConnected ? "healthy" : "unhealthy";

        setStatus(status);
      } catch (err: any) {
        if (!abortSignal.aborted) {
          console.error("ERROR: " + err);
          setStatus("error");
        }
      }
    }

    fetchStatus();

    return () => {
      abortController.abort();
    };
  }, []);

  return (
    <main>
      <p className="homeLink">
        Go Home: <a href="/">"/"</a>
      </p>
      <h1>API Health Check</h1>
      <div className="statusContainer">
        <span>Status:</span>
        <span className="apiStatus">{STATUS_MESSAGES[status]}</span>
      </div>
    </main>
  );
}
