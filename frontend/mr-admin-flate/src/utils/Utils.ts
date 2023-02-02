import { Tiltakstype } from "mulighetsrommet-api-client";

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

export function kalkulerStatusForTiltakstype(
  tiltakstype: Tiltakstype
): "Aktiv" | "Planlagt" | "Avsluttet" | " - " {
  const { fraDato, tilDato } = tiltakstype;
  const fraDatoAsDate = new Date(fraDato);
  const tilDatoAsDate = new Date(tilDato);
  const now = new Date();

  if (now >= fraDatoAsDate && now <= tilDatoAsDate) {
    return "Aktiv";
  } else if (now < fraDatoAsDate) {
    return "Planlagt";
  } else if (now > tilDatoAsDate) {
    return "Avsluttet";
  } else {
    return " - ";
  }
}
