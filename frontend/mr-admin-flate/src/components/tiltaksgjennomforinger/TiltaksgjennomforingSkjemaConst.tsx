import { isTiltakMedFellesOppstart } from "../../utils/tiltakskoder";
import {
  Avtale,
  Opphav,
  Tiltaksgjennomforing,
  TiltaksgjennomforingKontaktpersoner,
  TiltaksgjennomforingOppstartstype,
  Virksomhet,
} from "mulighetsrommet-api-client";
import { TiltaksgjennomforingUtkastData } from "./TiltaksgjennomforingSkjemaPage";
import { InferredTiltaksgjennomforingSchema } from "./TiltaksgjennomforingSchema";
import { DeepPartial } from "react-hook-form";

export function defaultOppstartType(avtale?: Avtale): TiltaksgjennomforingOppstartstype {
  if (!avtale) {
    return TiltaksgjennomforingOppstartstype.LOPENDE;
  }

  const tiltakskode = avtale.tiltakstype.arenaKode;
  return isTiltakMedFellesOppstart(tiltakskode)
    ? TiltaksgjennomforingOppstartstype.FELLES
    : TiltaksgjennomforingOppstartstype.LOPENDE;
}

export function defaultValuesForKontaktpersoner(
  kontaktpersoner?: TiltaksgjennomforingKontaktpersoner[],
): TiltaksgjennomforingKontaktpersoner[] {
  if (!kontaktpersoner) return [{ navIdent: "", navEnheter: [] }];

  return kontaktpersoner?.map((person) => ({
    navIdent: person.navIdent,
    navEnheter: person.navEnheter,
  }));
}

export const erArenaOpphav = (tiltaksgjennomforing: Tiltaksgjennomforing | undefined) => {
  return tiltaksgjennomforing?.opphav === Opphav.ARENA;
};

export const arrangorUnderenheterOptions = (avtale: Avtale, virksomhet: Virksomhet | undefined) => {
  const options =
    avtale?.leverandorUnderenheter.map((lev: any) => {
      return {
        label: `${lev.navn} - ${lev.organisasjonsnummer}`,
        value: lev.organisasjonsnummer,
      };
    }) || [];

  // Ingen underenheter betyr at alle er valgt, må gi valg om alle underenheter fra virksomhet
  if (options?.length === 0) {
    const enheter = virksomhet?.underenheter || [];
    return enheter.map((enhet) => ({
      value: enhet.organisasjonsnummer,
      label: `${enhet?.navn} - ${enhet?.organisasjonsnummer}`,
    }));
  }
  return options;
};

function defaultNavRegion(
  avtale: Avtale,
  tiltaksgjennomforing?: Tiltaksgjennomforing,
): string | undefined {
  if (tiltaksgjennomforing?.navRegion) {
    return tiltaksgjennomforing.navRegion.enhetsnummer;
  }
  if (avtale.kontorstruktur.length === 1) {
    return avtale.kontorstruktur[0].region.enhetsnummer;
  }
}

function defaultNavEnheter(avtale: Avtale, tiltaksgjennomforing?: Tiltaksgjennomforing): string[] {
  if (tiltaksgjennomforing?.navEnheter) {
    return tiltaksgjennomforing.navEnheter.map((enhet) => enhet.enhetsnummer);
  }
  if (avtale.kontorstruktur.length === 1) {
    return avtale.kontorstruktur[0].kontorer.map((enhet) => enhet.enhetsnummer);
  }
  return [];
}

function defaultArrangor(
  avtale: Avtale,
  tiltaksgjennomforing?: Tiltaksgjennomforing,
): string | undefined {
  if (tiltaksgjennomforing?.arrangor.organisasjonsnummer) {
    return tiltaksgjennomforing?.arrangor.organisasjonsnummer;
  }
  if (avtale.leverandorUnderenheter.length === 1) {
    return avtale.leverandorUnderenheter[0].organisasjonsnummer;
  }
}

export function fremmoteDatoFromTidspunkt(fremmoteTidspunkt?: string): string | undefined {
  return fremmoteTidspunkt ? new Date(fremmoteTidspunkt).toISOString().split("T")[0] : undefined;
}

export function fremmoteTidFromTidspunkt(fremmoteTidspunkt?: string): string | undefined {
  if (!fremmoteTidspunkt) return undefined;
  const d = new Date(fremmoteTidspunkt);

  return `${d.getHours()}:${d.getMinutes()}`;
}

export function utkastDataEllerDefault(
  avtale: Avtale,
  utkast?: TiltaksgjennomforingUtkastData,
  tiltaksgjennomforing?: Tiltaksgjennomforing,
): DeepPartial<InferredTiltaksgjennomforingSchema> {
  return {
    navn: tiltaksgjennomforing?.navn || avtale.navn,
    avtaleId: avtale.id,
    navRegion: defaultNavRegion(avtale, tiltaksgjennomforing),
    navEnheter: defaultNavEnheter(avtale, tiltaksgjennomforing),
    administratorer: tiltaksgjennomforing?.administratorer?.map((admin) => admin.navIdent),
    antallPlasser: tiltaksgjennomforing?.antallPlasser,
    startOgSluttDato: {
      startDato: tiltaksgjennomforing?.startDato,
      sluttDato: tiltaksgjennomforing?.sluttDato,
    },
    tiltaksArrangorUnderenhetOrganisasjonsnummer: defaultArrangor(avtale, tiltaksgjennomforing),
    midlertidigStengt: {
      erMidlertidigStengt: Boolean(tiltaksgjennomforing?.stengtFra),
      stengtFra: tiltaksgjennomforing?.stengtFra
        ? new Date(tiltaksgjennomforing.stengtFra)
        : undefined,
      stengtTil: tiltaksgjennomforing?.stengtTil
        ? new Date(tiltaksgjennomforing.stengtTil)
        : undefined,
    },
    oppstart: tiltaksgjennomforing?.oppstart || defaultOppstartType(avtale),
    apentForInnsok: tiltaksgjennomforing?.apentForInnsok,
    kontaktpersoner: defaultValuesForKontaktpersoner(tiltaksgjennomforing?.kontaktpersoner),
    stedForGjennomforing: tiltaksgjennomforing?.stedForGjennomforing ?? null,
    arrangorKontaktpersonId: tiltaksgjennomforing?.arrangor?.kontaktperson?.id,
    beskrivelse: tiltaksgjennomforing?.beskrivelse ?? null,
    faneinnhold: tiltaksgjennomforing?.faneinnhold ?? {
      forHvem: null,
      forHvemInfoboks: null,
      pameldingOgVarighet: null,
      pameldingOgVarighetInfoboks: null,
      detaljerOgInnhold: null,
      detaljerOgInnholdInfoboks: null,
    },
    opphav: tiltaksgjennomforing?.opphav ?? Opphav.MR_ADMIN_FLATE,
    fremmoteDato: fremmoteDatoFromTidspunkt(tiltaksgjennomforing?.fremmoteTidspunkt),
    fremmoteTid: fremmoteTidFromTidspunkt(tiltaksgjennomforing?.fremmoteTidspunkt),
    ...utkast,
  };
}
