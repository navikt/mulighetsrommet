import { Avtale, Avtaletype, EstimertVentetid } from "mulighetsrommet-api-client";
import { AvtaleFilter } from "../api/atoms";

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
): "Avtale" | "Rammeavtale" | "Forhåndsgodkjent" | "Offentlig-Offentlig samarbeid" {
  switch (type) {
    case Avtaletype.AVTALE:
      return "Avtale";
    case Avtaletype.FORHAANDSGODKJENT:
      return "Forhåndsgodkjent";
    case Avtaletype.RAMMEAVTALE:
      return "Rammeavtale";
    case Avtaletype.OFFENTLIG_OFFENTLIG:
      return "Offentlig-Offentlig samarbeid";
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

function specialChar(string: string | { label: string }) {
  return string
    .toString()
    .toLowerCase()
    .split("æ")
    .join("ae")
    .split("ø")
    .join("o")
    .split("å")
    .join("a");
}

export function kebabCase(string: string | { label: string }) {
  return specialChar(string).trim().replace(/\s+/g, "-").replace(/_/g, "-");
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

  filter.tiltakstyper.forEach((tiltakstype) => queryParams.append("tiltakstypeIder", tiltakstype));
  filter.statuser.forEach((status) => queryParams.append("statuser", status));
  filter.navRegioner.forEach((region) => queryParams.append("navRegioner", region));
  filter.leverandor.forEach((orgnr) => queryParams.append("leverandorOrgnr", orgnr));

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
