export function capitalize(text?: string): string {
  return text
    ? text.slice(0, 1).toUpperCase() + text.slice(1, text.length).toLowerCase()
    : "";
}

export function formaterDato(dato?: string | Date, fallback = ""): string {
  if (!dato) return fallback;

  const result = new Date(dato).toLocaleString("no-NO", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });

  if (result === "Invalid Date") {
    return fallback;
  }

  return result;
}
