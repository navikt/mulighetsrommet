import {
  EstimertVentetidEnhet,
  Tiltakskode,
  VeilderflateArrangor,
  VeilederflateTiltakstype,
} from "mulighetsrommet-api-client";

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

export function formatertVentetid(verdi: number, enhet: EstimertVentetidEnhet): string {
  switch (enhet) {
    case EstimertVentetidEnhet.UKE:
      return `${verdi} ${verdi === 1 ? "uke" : "uker"}`;
    case EstimertVentetidEnhet.MANED:
      return `${verdi} ${verdi === 1 ? "måned" : "måneder"}`;
    default:
      return "Ukjent enhet for ventetid";
  }
}

export function lesbareTiltaksnavn(
  navn: string,
  tiltakstype: VeilederflateTiltakstype,
  arrangor?: VeilderflateArrangor,
): string {
  if (arrangor?.selskapsnavn) {
    const { selskapsnavn } = arrangor;
    const konstruerteNavn = {
      [Tiltakskode.ARBEIDSFORBEREDENDE_TRENING]: `Arbeidsforberedende trening hos ${selskapsnavn}`,
      [Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET]: `Varig tilrettelagt arbeid i skjermet virksomhet hos ${selskapsnavn}`,
      [Tiltakskode.OPPFOLGING]: `Oppfølging hos ${selskapsnavn}`,
      [Tiltakskode.AVKLARING]: `Avklaring hos ${selskapsnavn}`,
      [Tiltakskode.ARBEIDSRETTET_REHABILITERING]: `Arbeidsrettet rehabilitering hos ${selskapsnavn}`,
      [Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK]: `Digital oppfølging hos ${selskapsnavn}`,
      [Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING]: `Kurs: ${navn} hos ${selskapsnavn}`,
      [Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING]: navn,
      [Tiltakskode.JOBBKLUBB]: `Jobbsøkerkurs hos ${selskapsnavn}`,
    };

    return (tiltakstype.tiltakskode && konstruerteNavn[tiltakstype.tiltakskode]) || navn;
  }

  return navn;
}
