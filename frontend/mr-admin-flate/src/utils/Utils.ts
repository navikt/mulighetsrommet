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
  NavEnhetDto,
  TilsagnAvvisningAarsak,
  TilsagnTilAnnulleringAarsak,
  TilsagnType,
  Tiltakskode,
  TiltakskodeArena,
  UtbetalingLinje,
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
    case Avtaletype.FORHANDSGODKJENT:
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

/**
 * @deprecated bruk `addDuration(date, {years: number})`
 */
export function addYear(date: Date, numYears: number): Date {
  const newDate = new Date(date);
  newDate.setFullYear(date.getFullYear() + numYears);
  return newDate;
}

/**
 * @deprecated Bruk `subDuration(date, {months: number})`
 */
export function subtractMonths(date: Date, numMonths: number): Date {
  const newDate = new Date(date);
  newDate.setMonth(date.getMonth() - numMonths);
  return newDate;
}

/**
 * @deprecated bruk `addDuration(date, {days: number})`
 */
export function addDays(date: Date | string, numDays: number): Date {
  const newDate = new Date(date);
  newDate.setDate(newDate.getDate() + numDays);
  return newDate;
}

/**
 * @deprecated Bruk `subDuration(date, {days: number})`
 */
export function subtractDays(date: Date | string, numDays: number): Date {
  const newDate = new Date(date);
  newDate.setDate(newDate.getDate() - numDays);
  return newDate;
}

export function avtaleHarRegioner(avtale: AvtaleDto): boolean {
  return avtale.kontorstruktur.some((stru) => stru.region);
}

/**
 * @deprecated Bruk maxOf([fra,til,annet])
 */
export function max(a: Date, b: Date): Date {
  return a > b ? a : b;
}

export function sorterPaRegionsnavn(a: { region: NavEnhetDto }, b: { region: NavEnhetDto }) {
  return a.region.navn.localeCompare(b.region.navn);
}

export function formaterNavEnheter(
  navEnheter: {
    navn: string;
    enhetsnummer: string;
  }[],
) {
  if (navEnheter.length < 1) return "-";

  const forsteEnhet = navEnheter.shift();

  return `${forsteEnhet?.navn} ${navEnheter.length > 0 ? `+ ${navEnheter.length}` : ""}`;
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
    case TilsagnTilAnnulleringAarsak.TILTAK_SKAL_IKKE_GJENNOMFORES:
      return "Tiltaket skal ikke gjennomføres";
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
    case DelutbetalingReturnertAarsak.PROPAGERT_RETUR:
      return "Automatisk returnert som følge av at en annen utbetalingslinje ble returnert";
    case DelutbetalingReturnertAarsak.TILSAGN_FEIL_STATUS:
      return "Tilsagnet har ikke lenger status godkjent og kan derfor ikke benyttes for utbetaling";
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
export function utbetalingLinjeCompareFn(linje1: UtbetalingLinje, linje2: UtbetalingLinje): number {
  return linje1.tilsagn.bestillingsnummer.localeCompare(linje2.tilsagn.bestillingsnummer);
}
