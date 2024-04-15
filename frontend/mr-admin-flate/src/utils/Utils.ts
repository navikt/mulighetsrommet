import { Avtale, Avtaletype, EstimertVentetid, Personopplysning } from "mulighetsrommet-api-client";
import { AvtaleFilter } from "@/api/atoms";

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

export const inneholderUrl = (string: string) => {
  return window.location.href.indexOf(string) > -1;
};

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

export const validEmail = (email: string | undefined): Boolean => {
  if (!email) return false;
  return Boolean(
    email
      .toLowerCase()
      .match(
        /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|.(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/,
      ),
  );
};

export function addYear(date: Date, numYears: number): Date {
  const newDate = new Date(date);
  newDate.setFullYear(date.getFullYear() + numYears);
  return newDate;
}

export function avtaleHarRegioner(avtale: Avtale): boolean {
  return avtale.kontorstruktur.some((stru) => stru.region);
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

export function createQueryParamsForExcelDownload(filter: AvtaleFilter): URLSearchParams {
  const queryParams = new URLSearchParams();

  if (filter.sok) {
    queryParams.set("search", filter.sok);
  }

  filter.tiltakstyper.forEach((tiltakstype) => queryParams.append("tiltakstyper", tiltakstype));
  filter.statuser.forEach((status) => queryParams.append("statuser", status));
  filter.avtaletyper.forEach((type) => queryParams.append("avtaletyper", type));
  filter.navRegioner.forEach((region) => queryParams.append("navRegioner", region));
  filter.arrangorer.forEach((arrangorId) => queryParams.append("arrangorer", arrangorId));

  if (filter.visMineAvtaler) {
    queryParams.set("visMineAvtaler", "true");
  }

  queryParams.set("size", "10000");
  return queryParams;
}

export function formatertVentetid(verdi: number, enhet: EstimertVentetid.enhet): string {
  switch (enhet) {
    case EstimertVentetid.enhet.UKE:
      return `${verdi} ${verdi === 1 ? "uke" : "uker"}`;
    case EstimertVentetid.enhet.MANED:
      return `${verdi} ${verdi === 1 ? "måned" : "måneder"}`;
    default:
      return "Ukjent enhet for ventetid";
  }
}

export function personopplysningToTekst(personopplysning: Personopplysning): string {
  switch (personopplysning) {
    case Personopplysning.NAVN:
      return "Navn";
    case Personopplysning.KJONN:
      return "Kjønn";
    case Personopplysning.ADRESSE:
      return "Adresse";
    case Personopplysning.TELEFONNUMMER:
      return "Telefonnummer";
    case Personopplysning.FOLKEREGISTER_IDENTIFIKATOR:
      return "Folkeregisteridentifikator";
    case Personopplysning.FODSELSDATO:
      return "Fødselsdato";
    case Personopplysning.BEHOV_FOR_BISTAND_FRA_NAV:
      return "Behov for bistand fra NAV";
    case Personopplysning.YTELSER_FRA_NAV:
      return "Ytelser fra NAV";
    case Personopplysning.BILDE:
      return "Bilde";
    case Personopplysning.EPOST:
      return "E-postadresse";
    case Personopplysning.BRUKERNAVN:
      return "Brukernavn";
    case Personopplysning.ARBEIDSERFARING_OG_VERV:
      return "Opplysninger knyttet til arbeidserfaring og verv som normalt fremkommer av en CV, herunder arbeidsgiver og hvor lenge man har jobbet";
    case Personopplysning.SERTIFIKATER_OG_KURS:
      return "Sertifikater og kurs, eks. Førerkort, vekterkurs";
    case Personopplysning.IP_ADRESSE:
      return "IP-adresse";
    case Personopplysning.UTDANNING_OG_FAGBREV:
      return "Utdanning, herunder fagbrev, høyere utdanning, grunnskoleopplæring osv.";
    case Personopplysning.PERSONLIGE_EGENSKAPER_OG_INTERESSER:
      return "Opplysninger om personlige egenskaper og interesser";
    case Personopplysning.SPRAKKUNNSKAP:
      return "Opplysninger om språkkunnskap";
    case Personopplysning.ADFERD:
      return "Opplysninger om atferd som kan ha betydning for tiltaksgjennomføring og jobbmuligheter (eks. truende adferd, vanskelig å samarbeide med osv.)";
    case Personopplysning.SOSIALE_FORHOLD:
      return "Sosiale eller personlige forhold som kan ha betydning for tiltaksgjennomføring og jobbmuligheter (eks. Aleneforsørger og kan derfor ikke jobbe kveldstid, eller økonomiske forhold som går ut over tiltaksgjennomføringen)";
    case Personopplysning.HELSEOPPLYSNINGER:
      return "Helseopplysninger";
    case Personopplysning.RELIGION:
      return "Religion";
  }
}
