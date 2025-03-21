import { useParams } from "react-router";
import { Periode, ProblemDetail, ValidationError } from "api-client";

export function formaterPeriode(periode: Periode) {
  const start = formaterDato(periode.start);
  const slutt = formaterDato(subtractDays(new Date(periode.slutt), 1));
  return `${start} - ${slutt}`;
}

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

export function subtractDays(date: Date, numDays: number): Date {
  const newDate = new Date(date);
  newDate.setDate(date.getDate() - numDays);
  return newDate;
}

export function problemDetailResponse(error: ProblemDetail): Response {
  return new Response(JSON.stringify(error), {
    status: error.status,
    headers: { "Content-Type": "application/json" },
  });
}

export function isValidationError(error: unknown): error is ValidationError {
  return typeof error === "object" && error !== null && "errors" in error;
}
