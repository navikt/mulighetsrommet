import { isTiltakMedFellesOppstart } from "../../utils/tiltakskoder";
import {
  Avtale,
  NavAnsatt,
  Opphav,
  Tiltaksgjennomforing,
  TiltaksgjennomforingOppstartstype,
  VirksomhetKontaktperson,
} from "mulighetsrommet-api-client";
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

export const erArenaOpphavOgIngenEierskap = (
  tiltaksgjennomforing: Tiltaksgjennomforing | undefined,
  migrerteTiltakstyper: string[],
) => {
  return (
    tiltaksgjennomforing?.opphav === Opphav.ARENA &&
    !migrerteTiltakstyper?.includes(tiltaksgjennomforing.tiltakstype.arenaKode)
  );
};

export const arrangorUnderenheterOptions = (avtale: Avtale) => {
  return (avtale?.leverandorUnderenheter ?? [])
    .sort((a, b) => a.navn.localeCompare(b.navn))
    .map((arrangor) => {
      return {
        label: `${arrangor.navn} - ${arrangor.organisasjonsnummer}`,
        value: arrangor.organisasjonsnummer,
      };
    });
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

export function defaultTiltaksgjennomforingData(
  ansatt: NavAnsatt,
  avtale: Avtale,
  tiltaksgjennomforing?: Tiltaksgjennomforing,
): DeepPartial<InferredTiltaksgjennomforingSchema> {
  return {
    navn: tiltaksgjennomforing?.navn || avtale.navn,
    avtaleId: avtale.id,
    navRegion: defaultNavRegion(avtale, tiltaksgjennomforing),
    navEnheter: defaultNavEnheter(avtale, tiltaksgjennomforing),
    administratorer: tiltaksgjennomforing?.administratorer?.map((admin) => admin.navIdent) || [
      ansatt.navIdent,
    ],
    antallPlasser: tiltaksgjennomforing?.antallPlasser,
    startOgSluttDato: {
      startDato: tiltaksgjennomforing?.startDato,
      sluttDato: tiltaksgjennomforing?.sluttDato,
    },
    tiltaksArrangorUnderenhetOrganisasjonsnummer: defaultArrangor(avtale, tiltaksgjennomforing),
    oppstart: tiltaksgjennomforing?.oppstart || defaultOppstartType(avtale),
    apentForInnsok: tiltaksgjennomforing?.apentForInnsok || true,
    kontaktpersoner: tiltaksgjennomforing?.kontaktpersoner ?? [],
    stedForGjennomforing: tiltaksgjennomforing?.stedForGjennomforing ?? null,
    arrangorKontaktpersoner:
      tiltaksgjennomforing?.arrangor?.kontaktpersoner.map((p: VirksomhetKontaktperson) => p.id) ??
      [],
    beskrivelse: tiltaksgjennomforing?.beskrivelse ?? avtale.beskrivelse,
    faneinnhold: tiltaksgjennomforing?.faneinnhold ?? avtale.faneinnhold,
    opphav: tiltaksgjennomforing?.opphav ?? Opphav.MR_ADMIN_FLATE,
    deltidsprosent: tiltaksgjennomforing?.deltidsprosent ?? 100,
    visEstimertVentetid: !!tiltaksgjennomforing?.estimertVentetid?.enhet,
    estimertVentetid: tiltaksgjennomforing?.estimertVentetid ?? null,
  };
}
