import {
  AmoKategoriseringInnholdElement as InnholdElement,
  AvtaleDto,
  Avtaletype,
  Bransje,
  UtbetalingLinjeReturnertAarsak,
  TilsagnStatusAarsak,
  TilsagnType,
  ValidationError,
  AvbrytGjennomforingAarsak,
  Tiltakskode,
  TilskuddOpplaeringType,
  GjennomforingType,
  TilskuddBehandlingStatusAarsak,
  KurstypeKode,
  TilskuddMottaker,
  BransjeKode,
} from "@tiltaksadministrasjon/api-client";
import { FieldErrors } from "react-hook-form";

export function capitalize(text?: string): string {
  return text ? text.slice(0, 1).toUpperCase() + text.slice(1, text.length).toLowerCase() : "";
}

export function capitalizeEveryWord(text: string = "", ignoreWords: string[] = []): string {
  return text
    .split(" ")
    .map((it) => {
      if (ignoreWords.includes(it.toLowerCase())) {
        return it.toLowerCase();
      }
      return capitalize(it);
    })
    .join(" ");
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

export function avtaleHarRegioner(avtale: AvtaleDto): boolean {
  return avtale.kontorstruktur.length > 0;
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

export function formatertVentetid(verdi: number, enhet: string): string {
  switch (enhet) {
    case "uke":
      return `${verdi} ${verdi === 1 ? "uke" : "uker"}`;
    case "maned":
      return `${verdi} ${verdi === 1 ? "måned" : "måneder"}`;
    default:
      return "Ukjent enhet for ventetid";
  }
}

export function opplaeringTilskuddToString(tilskuddType: TilskuddOpplaeringType): string {
  switch (tilskuddType) {
    case TilskuddOpplaeringType.EKSAMENSAVGIFT:
      return "Eksamensavgift";
    case TilskuddOpplaeringType.INTEGRERT_BOTILBUD:
      return "Integrert botilbud";
    case TilskuddOpplaeringType.SEMESTERAVGIFT:
      return "Semesteravgift";
    case TilskuddOpplaeringType.SKOLEPENGER:
      return "Skolepenger";
    case TilskuddOpplaeringType.STUDIEREISE:
      return "Studiereise";
  }
}

export function tilskuddMottakerToString(mottaker: TilskuddMottaker): string {
  switch (mottaker) {
    case TilskuddMottaker.BRUKER:
      return "Utbetales til brukeren";
    case TilskuddMottaker.ARRANGOR:
      return "Utbetales til arrangøren";
  }
}

export function bransjeToString(bransje: Bransje): string {
  switch (bransje.kode) {
    case BransjeKode.INGENIOR_OG_IKT_FAG:
      return "Ingeniør- og IKT-fag";
    case BransjeKode.HELSE_PLEIE_OG_OMSORG:
      return "Helse, pleie og omsorg";
    case BransjeKode.BARNE_OG_UNGDOMSARBEID:
      return "Barne- og ungdomsarbeid";
    case BransjeKode.KONTORARBEID:
      return "Kontorarbeid";
    case BransjeKode.BUTIKK_OG_SALGSARBEID:
      return "Butikk- og salgsarbeid";
    case BransjeKode.BYGG_OG_ANLEGG:
      return "Bygg og anlegg";
    case BransjeKode.INDUSTRIARBEID:
      return "Industriarbeid";
    case BransjeKode.REISELIV_SERVERING_OG_TRANSPORT:
      return "Reiseliv, servering og transport";
    case BransjeKode.SERVICEYRKER_OG_ANNET_ARBEID:
      return "Serviceyrker og annet arbeid";
    case BransjeKode.ANDRE_BRANSJER:
      return "Andre bransjer";
  }
}

export function innholdElementToString(innholdElement: InnholdElement): string {
  switch (innholdElement) {
    case InnholdElement.BRANSJERETTET_OPPLARING:
      return "Bransjerettet opplæring";
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

export function getPublisertStatus(statuser: string[] = []): boolean | undefined {
  if (statuser.length === 0) return undefined;

  if (statuser.every((status) => status === "publisert")) return true;

  if (statuser.every((status) => status === "ikke-publisert")) return false;

  return undefined;
}

export function aarsakTilTekst(
  aarsak: TilsagnStatusAarsak | TilskuddBehandlingStatusAarsak,
): string {
  switch (aarsak) {
    case TilskuddBehandlingStatusAarsak.FEIL_SAKSOPPLYSNINGER:
      return "Feil i saksopplysninger";
    case TilskuddBehandlingStatusAarsak.FEIL_VEDTAKSRESULTAT:
      return "Feil vedtaksresultat";
    case TilsagnStatusAarsak.FEIL_PERIODE:
      return "Feil periode";
    case TilsagnStatusAarsak.FEIL_ANTALL_PLASSER:
      return "Feil antall plasser";
    case TilsagnStatusAarsak.FEIL_KOSTNADSSTED:
      return "Feil kostnadssted";
    case TilsagnStatusAarsak.FEIL_BELOP:
    case TilskuddBehandlingStatusAarsak.FEIL_BELOP:
      return "Feil beløp";
    case TilsagnStatusAarsak.FEIL_REGISTRERING:
      return "Feilregistrering";
    case TilsagnStatusAarsak.TILTAK_SKAL_IKKE_GJENNOMFORES:
      return "Tiltaket skal ikke gjennomføres";
    case TilsagnStatusAarsak.ARRANGOR_HAR_IKKE_SENDT_KRAV:
      return "Arrangør har ikke sendt krav";
    case TilsagnStatusAarsak.ANNET:
    case TilskuddBehandlingStatusAarsak.ANNET:
      return "Annet";
  }
}

export function utbetalingLinjeAarsakTilTekst(aarsak: UtbetalingLinjeReturnertAarsak): string {
  switch (aarsak) {
    case UtbetalingLinjeReturnertAarsak.FEIL_BELOP:
      return "Feil beløp";
    case UtbetalingLinjeReturnertAarsak.ANNET:
      return "Annet";
    case UtbetalingLinjeReturnertAarsak.PROPAGERT_RETUR:
      return "Automatisk returnert som følge av at en annen utbetalingslinje ble returnert";
    case UtbetalingLinjeReturnertAarsak.TILSAGN_FEIL_STATUS:
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

export function isValidationError(error: unknown): error is ValidationError {
  return typeof error === "object" && error !== null && "errors" in error;
}

export function avbrytGjennomforingAarsakTilTekst(aarsak: AvbrytGjennomforingAarsak): string {
  switch (aarsak) {
    case AvbrytGjennomforingAarsak.BUDSJETT_HENSYN:
      return "Budsjetthensyn";
    case AvbrytGjennomforingAarsak.ENDRING_HOS_ARRANGOR:
      return "Endring hos arrangør";
    case AvbrytGjennomforingAarsak.FEILREGISTRERING:
      return "Feilregistrering";
    case AvbrytGjennomforingAarsak.FOR_FAA_DELTAKERE:
      return "For få deltakere";
    case AvbrytGjennomforingAarsak.AVBRUTT_I_ARENA:
      return "Avbrutt i Arena";
    case AvbrytGjennomforingAarsak.ANNET:
      return "Annet";
  }
}

export type ValidationMessage = {
  message: string;
  ref?: string;
};

export function extractValidationErrors(errors: FieldErrors): ValidationMessage[] {
  const messages: ValidationMessage[] = [];

  for (const key in errors) {
    const error = errors[key];

    if (!error || typeof error !== "object") {
      continue;
    }

    if ("message" in error && typeof error.message === "string") {
      messages.push({
        message: error.message,
      });
    } else {
      messages.push(...extractValidationErrors(error as FieldErrors));
    }
  }

  return messages;
}

export function kursOgTiltakErStudiespesialisering(
  amo: KurstypeKode | undefined,
  tiltakskode: Tiltakskode,
) {
  return (
    amo === KurstypeKode.STUDIESPESIALISERING && tiltakskode === Tiltakskode.STUDIESPESIALISERING
  );
}

export function gjennomforingTypeToString(type: GjennomforingType): string {
  switch (type) {
    case GjennomforingType.ARENA:
      return "Arena";
    case GjennomforingType.AVTALE:
      return "Gruppe";
    case GjennomforingType.ENKELTPLASS:
      return "Enkeltplass";
  }
}
