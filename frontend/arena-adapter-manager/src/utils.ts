export function isDevelopment() {
  const hostname = window.location.hostname;
  return (
    hostname.endsWith(".intern.dev.nav.no") || hostname === "localhost" || hostname === "127.0.0.1"
  );
}

export function formatUTCDate(str: string | null) {
  if (!str) {
    return "";
  }
  const date = new Date(str);
  return new Intl.DateTimeFormat("nb-NO", {
    dateStyle: "short",
    timeStyle: "medium",
    timeZone: "Europe/Oslo",
  }).format(date);
}
