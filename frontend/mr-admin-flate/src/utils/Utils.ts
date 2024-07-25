import {
  AvbrytAvtaleAarsak,
  Avtale,
  Avtaletype,
  EstimertVentetidEnhet,
  ForerkortKlasse,
  InnholdElement,
  Kurstype,
  Spesifisering,
} from "mulighetsrommet-api-client";
import { AvtaleFilter, TiltaksgjennomforingFilter } from "@/api/atoms";

export function capitalize(text?: string): string {
  return text ? text.slice(0, 1).toUpperCase() + text.slice(1, text.length).toLowerCase() : "";
}

export function capitalizeEveryWord(text: string = "", ignoreWords: string[] = []): string {
  return text
    ?.split(" ")
    ?.map((it) => {
      if (ignoreWords.includes(it.toLowerCase())) {
        return it.toLowerCase();
      }
      return capitalize(it);
    })
    ?.join(" ");
}

export function formaterDato(dato: string | Date): string {
  const result = new Date(dato).toLocaleString("no-NO", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });

  return result;
}

export function formaterDatoTid(dato: string | Date, fallback = ""): string {
  const result = new Date(dato).toLocaleTimeString("no-NO", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });

  if (result === "Invalid Date") {
    return fallback;
  }

  return result.replace(",", " ");
}

export function formaterDatoSomYYYYMMDD(dato?: Date | null, fallback = ""): string {
  if (!dato) return fallback;
  const year = dato.getFullYear();
  const month = (dato.getMonth() + 1).toString();
  const day = dato.getDate().toString();
  return year + "-" + (month[1] ? month : "0" + month[0]) + "-" + (day[1] ? day : "0" + day[0]);
  // https://stackoverflow.com/questions/23593052/format-javascript-date-as-yyyy-mm-dd
}

export function formaterTall(tall: number) {
  return Intl.NumberFormat("no-nb").format(tall);
}

export function kalkulerStatusBasertPaaFraOgTilDato(
  datoer: {
    fraDato: string;
    tilDato: string;
  },
  now: Date = new Date(),
): "Aktiv" | "Planlagt" | "Avsluttet" | " - " {
  const { fraDato, tilDato } = datoer;
  const fraDatoAsDate = new Date(fraDato);
  const tilDatoAsDate = new Date(tilDato);

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

export function inneholderUrl(string: string) {
  return window.location.href.indexOf(string) > -1;
}

export function avtaletypeTilTekst(
  type: Avtaletype,
): "Avtale" | "Rammeavtale" | "Forhåndsgodkjent" | "Offentlig-offentlig samarbeid" {
  switch (type) {
    case Avtaletype.AVTALE:
      return "Avtale";
    case Avtaletype.FORHAANDSGODKJENT:
      return "Forhåndsgodkjent";
    case Avtaletype.RAMMEAVTALE:
      return "Rammeavtale";
    case Avtaletype.OFFENTLIG_OFFENTLIG:
      return "Offentlig-offentlig samarbeid";
  }
}

export function valueOrDefault<T, X>(value: T | undefined, defaultValue: X): T | X {
  return value !== undefined ? value : defaultValue;
}

export function validEmail(email: string | undefined): Boolean {
  if (!email) return false;
  return Boolean(
    email
      .toLowerCase()
      .match(
        /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|.(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/,
      ),
  );
}

export function addYear(date: Date, numYears: number): Date {
  const newDate = new Date(date);
  newDate.setFullYear(date.getFullYear() + numYears);
  return newDate;
}

export function subtractMonths(date: Date, numMonths: number): Date {
  const newDate = new Date(date);
  newDate.setMonth(date.getMonth() - numMonths);
  return newDate;
}

export function addDays(date: Date, numDays: number): Date {
  const newDate = new Date(date);
  newDate.setDate(date.getDate() + numDays);
  return newDate;
}

export function subtractDays(date: Date, numDays: number): Date {
  const newDate = new Date(date);
  newDate.setDate(date.getDate() - numDays);
  return newDate;
}

export function avtaleHarRegioner(avtale: Avtale): boolean {
  return avtale.kontorstruktur.some((stru) => stru.region);
}

export function max(a: Date, b: Date): Date {
  return a > b ? a : b;
}

export function formaterNavEnheter(
  navRegionNavn: string = "",
  navEnheter?: {
    navn?: string | null;
    enhetsnummer?: string;
  }[],
) {
  const liste = [...(navEnheter || [])];
  if (!liste) return "";

  const forsteEnhet = liste.shift();
  if (!forsteEnhet) return navRegionNavn;

  return `${forsteEnhet?.navn} ${liste.length > 0 ? `+ ${liste.length}` : ""}`;
}

export function addOrRemove<T>(array: T[], item: T): T[] {
  const exists = array.includes(item);

  if (exists) {
    return array.filter((c) => {
      return c !== item;
    });
  } else {
    return [...array, item];
  }
}

export function createQueryParamsForExcelDownloadForAvtale(filter: AvtaleFilter): URLSearchParams {
  const queryParams = new URLSearchParams();

  if (filter.sok) {
    queryParams.set("search", filter.sok);
  }

  filter.tiltakstyper.forEach((tiltakstype) => queryParams.append("tiltakstyper", tiltakstype));
  filter.statuser.forEach((status) => queryParams.append("statuser", status));
  filter.avtaletyper.forEach((type) => queryParams.append("avtaletyper", type));
  filter.navRegioner.forEach((region) => queryParams.append("navRegioner", region));
  filter.arrangorer.forEach((arrangorId) => queryParams.append("arrangorer", arrangorId));
  filter.personvernBekreftet.forEach((b) =>
    queryParams.append("personvernBekreftet", b ? "true" : "false"),
  );

  if (filter.visMineAvtaler) {
    queryParams.set("visMineAvtaler", "true");
  }

  queryParams.set("size", "10000");
  return queryParams;
}

export function createQueryParamsForExcelDownloadForTiltaksgjennomforing(
  filter: TiltaksgjennomforingFilter,
): URLSearchParams {
  const queryParams = new URLSearchParams();

  if (filter.search) {
    queryParams.set("search", filter.search);
  }

  filter.navEnheter.forEach((navEnhet) => queryParams.append("navEnheter", navEnhet));
  filter.tiltakstyper.forEach((tiltakstype) => queryParams.append("tiltakstyper", tiltakstype));
  filter.statuser.forEach((status) => queryParams.append("statuser", status));

  if (filter.avtale) {
    queryParams.set("avtale", filter.avtale);
  }

  filter.arrangorer.forEach((arrangorId) => queryParams.append("arrangorer", arrangorId));

  if (filter.visMineGjennomforinger) {
    queryParams.set("visMineGjennomforinger", "true");
  }

  queryParams.set("size", "10000");
  return queryParams;
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

export function avbrytAvtaleAarsakToString(aarsak: AvbrytAvtaleAarsak | string): string {
  switch (aarsak) {
    case AvbrytAvtaleAarsak.AVBRUTT_I_ARENA:
      return "Avbrutt i Arena";
    case AvbrytAvtaleAarsak.BUDSJETT_HENSYN:
      return "Budsjetthensyn";
    case AvbrytAvtaleAarsak.ENDRING_HOS_ARRANGOR:
      return "Endring hos arrangør";
    case AvbrytAvtaleAarsak.FEILREGISTRERING:
      return "Feilregistrering";
    default:
      return aarsak;
  }
}

export function forerkortKlasseToString(klasse: ForerkortKlasse): string {
  switch (klasse) {
    case ForerkortKlasse.A:
      return "A - Motorsykkel";
    case ForerkortKlasse.A1:
      return "A1 - Lett motorsykkel";
    case ForerkortKlasse.A2:
      return "A2 - Mellomtung motorsykkel";
    case ForerkortKlasse.AM:
      return "AM - Moped";
    case ForerkortKlasse.AM_147:
      return "AM 147 - Mopedbil";
    case ForerkortKlasse.B:
      return "B - Personbil";
    case ForerkortKlasse.B_78:
      return "B 78 - Personbil med automatgir";
    case ForerkortKlasse.BE:
      return "BE - Personbil med tilhenger";
    case ForerkortKlasse.C:
      return "C - Lastebil";
    case ForerkortKlasse.C1:
      return "C1 - Lett lastebil";
    case ForerkortKlasse.C1E:
      return "C1E - Lett lastebil med tilhenger";
    case ForerkortKlasse.CE:
      return "CE - Lastebil med tilhenger";
    case ForerkortKlasse.D:
      return "D - Buss";
    case ForerkortKlasse.D1:
      return "D1 - Minibuss";
    case ForerkortKlasse.D1E:
      return "D1E - Minibuss med tilhenger";
    case ForerkortKlasse.DE:
      return "DE - Buss med tilhenger";
    case ForerkortKlasse.S:
      return "S - Snøscooter";
    case ForerkortKlasse.T:
      return "T - Traktor";
  }
}

export function kurstypeToString(kurstype: Kurstype): string {
  switch (kurstype) {
    case Kurstype.BRANSJE:
      return "Bransje-/yrkesrettede kurs";
    case Kurstype.NORSKOPPLAERING:
      return "Norskopplæring/grunnleggende ferdigheter";
    case Kurstype.STUDIESPESIALISERING:
      return "Studiespesialisering";
  }
}

export function spesifiseringToString(spesifisering: Spesifisering): string {
  switch (spesifisering) {
    case Spesifisering.SERVERING_OVERNATTING:
      return "Servering/overnatting";
    case Spesifisering.TRANSPORT:
      return "Transport";
    case Spesifisering.INDUSTRI:
      return "Industri";
    case Spesifisering.ANDRE_BRANSJER:
      return "Andre bransjer";
    case Spesifisering.NORSKOPPLAERING:
      return "Norskopplæring";
    case Spesifisering.GRUNNLEGGENDE_FERDIGHETER:
      return "Grunnleggende ferdigheter";
  }
}

export function innholdElementToString(innholdElement: InnholdElement): string {
  switch (innholdElement) {
    case InnholdElement.GRUNNLEGGENDE_FERDIGHETER:
      return "Grunnleggende ferdigheter";
    case InnholdElement.JOBBSOKER_KOMPETANSE:
      return "Jobbsøkerkompetanse";
    case InnholdElement.TEORETISK_OPPLAERING:
      return "Teoretisk opplæring";
    case InnholdElement.PRAKSIS:
      return "Praksis";
    case InnholdElement.ARBEIDSMARKEDSKUNNSKAP:
      return "Arbeidsmarkedskunnskap";
    case InnholdElement.NORSKOPPLAERING:
      return "Norskopplæring";
  }
}
