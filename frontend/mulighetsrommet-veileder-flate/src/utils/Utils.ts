import { VeilederflateTiltak } from "@mr/api-client";
import { isKursTiltak } from "@mr/frontend-common/utils/utils";

export function inneholderUrl(string: string) {
  return window.location.href.indexOf(string) > -1;
}

export function erPreview() {
  return inneholderUrl("/preview");
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

export function utledLopenummerFraTiltaksnummer(tiltaksnummer: string): string {
  const parts = tiltaksnummer.split("#");
  if (parts.length >= 2) {
    return parts[1];
  }

  return tiltaksnummer;
}

export function addOrRemove<T>(array: T[], item: T): T[] {
  const exists = array.includes(item);

  if (exists) {
    return array.filter((c) => {
      return c !== item;
    });
  } else {
    const result = array;
    result.push(item);
    return result;
  }
}

export function visningNavn(tiltak: VeilederflateTiltak): string {
  const { tiltakstype, navn } = tiltak;

  if (isKursTiltak(tiltakstype.tiltakskode, tiltakstype.arenakode)) {
    return navn;
  }
  return tiltakstype.navn;
}

export function alternativNavn(tiltak: VeilederflateTiltak): string {
  const { tiltakstype, navn } = tiltak;

  if (isKursTiltak(tiltakstype.tiltakskode, tiltakstype.arenakode)) {
    return tiltakstype.navn;
  }
  return navn;
}
