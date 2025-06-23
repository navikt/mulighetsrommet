const ddMMyyyyFormat = new RegExp("^[0-9]{2}.[0-9]{2}.[0-9]{4}$");
const yyyyMMddFormat = new RegExp("^[0-9]{4}-[0-9]{2}-[0-9]{2}$");

export function formaterDatoSomYYYYMMDD(
  dato: string | Date | null | undefined,
  fallback = "",
): string {
  if (!dato) {
    return fallback;
  }

  if (typeof dato !== "string") {
    return dateToyyyyMMdd(dato, fallback);
  }

  if (yyyyMMddFormat.test(dato)) {
    return dato;
  }

  if (ddMMyyyyFormat.test(dato)) {
    const [day, month, year] = dato.split(".").map(Number);
    return dateToyyyyMMdd(new Date(year, month - 1, day), fallback);
  }

  return fallback;
}

function dateToyyyyMMdd(dato: Date, fallback: string): string {
  if (isNaN(dato.getTime())) return fallback;

  const year = dato.getFullYear();
  const month = String(dato.getMonth() + 1).padStart(2, "0");
  const day = String(dato.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}
