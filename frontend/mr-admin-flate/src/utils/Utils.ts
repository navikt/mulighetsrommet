import { AvtaleFilter, GjennomforingFilter } from "@/api/atoms";
import {
  AgentDto,
  AvbrytAvtaleAarsak,
  AvtaleDto,
  Avtaletype,
  Bransje,
  DelutbetalingReturnertAarsak,
  EstimertVentetidEnhet,
  ForerkortKlasse,
  InnholdElement,
  Kurstype,
  LastNedAvtalerSomExcelData,
  LastNedGjennomforingerSomExcelData,
  NavEnhet,
  Periode,
  TilsagnAvvisningAarsak,
  TilsagnTilAnnulleringAarsak,
  TilsagnType,
  Tiltakskode,
  TiltakskodeArena,
  ValidationError,
} from "@mr/api-client-v2";

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

export function compareByKey<T extends Record<string, any>>(a: T, b: T, orderBy: keyof T): number {
  const aValue = a[orderBy];
  const bValue = b[orderBy];

  if (aValue == null && bValue == null) {
    return 0;
  } else if (aValue == null) {
    return 1;
  } else if (bValue == null) {
    return -1;
  }

  if (typeof aValue === "number" && typeof bValue === "number") {
    return bValue - aValue;
  }

  if (bValue < aValue) {
    return -1;
  } else if (bValue > aValue) {
    return 1;
  } else {
    return 0;
  }
}

export function formaterPeriode(periode: Periode) {
  return `${formaterPeriodeStart(periode)} - ${formaterPeriodeSlutt(periode)}`;
}

export function formaterPeriodeStart(periode: Periode) {
  return formaterDato(periode.start);
}

export function formaterPeriodeSlutt(periode: Periode) {
  return formaterDato(subtractDays(new Date(periode.slutt), 1));
}

export function formaterDato(dato: string | Date | undefined | null): string {
  if (!dato) return "";

  return new Date(dato).toLocaleString("no-NO", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
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

export function validEmail(email: string | undefined): boolean {
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

export function addMonths(date: Date, numOfMonths: number): Date {
  const newDate = new Date(date);
  newDate.setMonth(date.getMonth() + numOfMonths);
  return newDate;
}

export function subtractMonths(date: Date, numMonths: number): Date {
  const newDate = new Date(date);
  newDate.setMonth(date.getMonth() - numMonths);
  return newDate;
}

export function addDays(date: Date | string, numDays: number): Date {
  const newDate = new Date(date);
  newDate.setDate(newDate.getDate() + numDays);
  return newDate;
}

export function subtractDays(date: Date | string, numDays: number): Date {
  const newDate = new Date(date);
  newDate.setDate(newDate.getDate() - numDays);
  return newDate;
}

export function avtaleHarRegioner(avtale: AvtaleDto): boolean {
  return avtale.kontorstruktur.some((stru) => stru.region);
}

export function max(a: Date, b: Date): Date {
  return a > b ? a : b;
}

export function sorterPaRegionsnavn(a: { region: NavEnhet }, b: { region: NavEnhet }) {
  return a.region.navn.localeCompare(b.region.navn);
}

export function formaterNavEnheter(
  navEnheter: {
    navn: string;
    enhetsnummer: string;
  }[],
) {
  if (navEnheter.length < 1) return "";

  const forsteEnhet = navEnheter.shift();

  return `${forsteEnhet?.navn} ${navEnheter.length > 0 ? `+ ${navEnheter.length}` : ""}`;
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

export function createQueryParamsForExcelDownloadForAvtale(
  filter: AvtaleFilter,
): Pick<LastNedAvtalerSomExcelData, "query"> {
  return {
    query: {
      search: filter.sok,
      tiltakstyper: filter.tiltakstyper,
      statuser: filter.statuser,
      avtaletyper: filter.avtaletyper,
      navRegioner: filter.navRegioner,
      arrangorer: filter.arrangorer,
      personvernBekreftet: filter.personvernBekreftet,
      visMineAvtaler: filter.visMineAvtaler,
      size: 10000,
    },
  };
}

export function createQueryParamsForExcelDownloadForGjennomforing(
  filter: GjennomforingFilter,
): Pick<LastNedGjennomforingerSomExcelData, "query"> {
  return {
    query: {
      search: filter.search,
      navEnheter: filter.navEnheter.map((enhet) => enhet.enhetsnummer),
      tiltakstyper: filter.tiltakstyper,
      statuser: filter.statuser,
      avtaleId: filter.avtale,
      arrangorer: filter.arrangorer,
      visMineTiltaksgjennomforinger: filter.visMineGjennomforinger,
      size: filter.pageSize,
      sort: filter.sortering.sortString,
      publisert: getPublisertStatus(filter.publisert),
    },
  };
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
    case Kurstype.BRANSJE_OG_YRKESRETTET:
      return "Bransje-/yrkesrettet kurs";
    case Kurstype.NORSKOPPLAERING:
      return "Norskopplæring";
    case Kurstype.STUDIESPESIALISERING:
      return "Studiespesialisering";
    case Kurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE:
      return "FOV (forberedende opplæring for voksne)";
    case Kurstype.GRUNNLEGGENDE_FERDIGHETER:
      return "Grunnleggende ferdigheter";
  }
}

export function bransjeToString(bransje: Bransje): string {
  switch (bransje) {
    case Bransje.INGENIOR_OG_IKT_FAG:
      return "Ingeniør- og IKT-fag";
    case Bransje.HELSE_PLEIE_OG_OMSORG:
      return "Helse, pleie og omsorg";
    case Bransje.BARNE_OG_UNGDOMSARBEID:
      return "Barne- og ungdomsarbeid";
    case Bransje.KONTORARBEID:
      return "Kontorarbeid";
    case Bransje.BUTIKK_OG_SALGSARBEID:
      return "Butikk- og salgsarbeid";
    case Bransje.BYGG_OG_ANLEGG:
      return "Bygg og anlegg";
    case Bransje.INDUSTRIARBEID:
      return "Industriarbeid";
    case Bransje.REISELIV_SERVERING_OG_TRANSPORT:
      return "Reiseliv, servering og transport";
    case Bransje.SERVICEYRKER_OG_ANNET_ARBEID:
      return "Serviceyrker og annet arbeid";
    case Bransje.ANDRE_BRANSJER:
      return "Andre bransjer";
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

export function getPublisertStatus(statuser: string[] = []): boolean | null {
  if (statuser.length === 0) return null;

  if (statuser.every((status) => status === "publisert")) return true;

  if (statuser.every((status) => status === "ikke-publisert")) return false;

  return null;
}

export function tilsagnAarsakTilTekst(
  aarsak: TilsagnAvvisningAarsak | TilsagnTilAnnulleringAarsak,
): string {
  switch (aarsak) {
    case TilsagnAvvisningAarsak.FEIL_PERIODE:
      return "Feil periode";
    case TilsagnAvvisningAarsak.FEIL_ANTALL_PLASSER:
      return "Feil antall plasser";
    case TilsagnAvvisningAarsak.FEIL_KOSTNADSSTED:
      return "Feil kostnadssted";
    case TilsagnAvvisningAarsak.FEIL_BELOP:
      return "Feil beløp";
    case TilsagnAvvisningAarsak.FEIL_ANNET:
      return "Annet";
    case TilsagnTilAnnulleringAarsak.FEIL_REGISTRERING:
      return "Feilregistrering";
    case TilsagnTilAnnulleringAarsak.GJENNOMFORING_AVBRYTES:
      return "Gjennomføring skal avbrytes";
    case TilsagnTilAnnulleringAarsak.ARRANGOR_HAR_IKKE_SENDT_KRAV:
      return "Arrangør har ikke sendt krav";
    case TilsagnTilAnnulleringAarsak.FEIL_ANNET:
      return "Annet";
  }
}

export function delutbetalingAarsakTilTekst(aarsak: DelutbetalingReturnertAarsak): string {
  switch (aarsak) {
    case DelutbetalingReturnertAarsak.FEIL_BELOP:
      return "Feil beløp";
    case DelutbetalingReturnertAarsak.FEIL_ANNET:
      return "Annet";
    case DelutbetalingReturnertAarsak.AUTOMATISK_RETURNERT:
      return "Automatisk returnert";
  }
}

export function tilsagnTypeToString(type: TilsagnType): string {
  switch (type) {
    case TilsagnType.TILSAGN:
      return "Tilsagn";
    case TilsagnType.EKSTRATILSAGN:
      return "Ekstratilsagn";
    case TilsagnType.INVESTERING:
      return "Investering";
  }
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

export function isValidationError(error: unknown): error is ValidationError {
  return typeof error === "object" && error !== null && "errors" in error;
}

export function joinWithCommaAndOg(aarsaker: string[]): string {
  if (aarsaker.length === 0) return "";
  if (aarsaker.length === 1) return aarsaker[0];
  return `${aarsaker.slice(0, -1).join(", ")} og ${aarsaker[aarsaker.length - 1]}`;
}

export function capitalizeFirstLetter(text: string): string {
  if (!text) return "";
  return text.charAt(0).toUpperCase() + text.slice(1).toLowerCase();
}

export function navnEllerIdent(agent: AgentDto): string {
  switch (agent.type) {
    case "NAV_ANSATT":
      return agent.navn || agent.navIdent;
    case "SYSTEM":
      return agent.navn;
    case "ARRANGOR":
      return "Tiltaksarrangør";
  }
}
