import { GjennomforingOppstartstype, NavRegionDto, VeilederflateTiltak } from "@api-client";
import { isTiltakEnkeltplass } from "@/api/queries/useArbeidsmarkedstiltakById";

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

export function utledOppstart(tiltak: VeilederflateTiltak) {
  if (isTiltakEnkeltplass(tiltak)) {
    return "Løpende oppstart";
  }

  switch (tiltak.oppstart) {
    case GjennomforingOppstartstype.FELLES:
      return formaterDato(tiltak.oppstartsdato);
    case GjennomforingOppstartstype.LOPENDE:
      return "Løpende oppstart";
  }
}

export function getSelectedNavEnheter(regioner: NavRegionDto[], enheter: string[]): string[] {
  return regioner
    .flatMap((region) => region.enheter)
    .filter((enhet) => enheter.includes(enhet.enhetsnummer))
    .map((enhet) => enhet.navn);
}
