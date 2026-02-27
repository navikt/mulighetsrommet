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
