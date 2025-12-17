/**
 * Builds a base HTTP URL.
 *
 * @param host Hostname or IP.
 * @param port Port number as string.
 * @returns "http://host:port" or null if either value is missing.
 */
export function buildUrl(host: string, port: string): string | null {
  return host && port ? `http://${host}:${port}` : null;
}
