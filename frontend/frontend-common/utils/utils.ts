import {
  GjennomforingStatus,
  Tiltakskode,
  TiltakskodeArena,
  ValidationError,
} from "@mr/api-client-v2";
import { shallowEquals } from "./shallow-equals";

export function addOrRemove<T>(array: T[], item: T): T[] {
  const exists = array.some((a) => shallowEquals(a, item));

  if (exists) {
    return array.filter((c) => {
      return !shallowEquals(c, item);
    });
  } else {
    const result = array;
    result.push(item);
    return result;
  }
}

export function gjennomforingIsAktiv(status: GjennomforingStatus): boolean {
  return status === GjennomforingStatus.GJENNOMFORES;
}

export function formaterNOK(tall: number) {
  return `${formaterTall(tall)} NOK`;
}

export function formaterTall(tall: number) {
  return Intl.NumberFormat("no-nb").format(tall);
}

export function isKursTiltak(tiltakskode?: Tiltakskode, arenaKode?: TiltakskodeArena): boolean {
  if (tiltakskode) {
    return [
      Tiltakskode.JOBBKLUBB,
      Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
      Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
    ].includes(tiltakskode);
  }

  if (arenaKode) {
    return [TiltakskodeArena.ENKELAMO, TiltakskodeArena.ENKFAGYRKE].includes(arenaKode);
  }

  return false;
}

export function formaterKontoNummer(kontoNummer?: string): string {
  return !kontoNummer
    ? ""
    : `${kontoNummer.substring(0, 4)} ${kontoNummer.substring(4, 6)} ${kontoNummer.substring(6, 11)}`;
}

export function isValidationError(error: unknown): error is ValidationError {
  return (
    typeof error === "object" &&
      error !== null &&
    "errors" in error
  );
}

export function jsonPointerToFieldPath(pointer: string): string {
  return pointer
    .replace(/^\//, "") // Remove leading slash
    .replace(/\/$/, "") // Remove trailing slash
    .split("/")          // Split by slash
    .filter(Boolean)     // Remove empty parts (handles double slashes)
    .join(".")           // Join with dots
    .replace(/~1/g, "/") // Decode escaped '/'
    .replace(/~0/g, "~"); // Decode escaped '~'
}
