import { useParams } from "react-router";

export function formaterDato(dato: string | Date, fallback = ""): string {
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

export function useOrgnrFromUrl() {
  const { orgnr } = useParams();

  if (!orgnr) {
    throw new Error("Fant ikke orgnr i url");
  }

  return orgnr;
}
