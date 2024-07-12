import { isTiltakMedFellesOppstart } from "../../utils/tiltakskoder";
import {
  ArrangorKontaktperson,
  Avtale,
  NavAnsatt,
  Opphav,
  Tiltaksgjennomforing,
  TiltaksgjennomforingOppstartstype,
} from "mulighetsrommet-api-client";
import { InferredTiltaksgjennomforingSchema } from "@/components/redaksjoneltInnhold/TiltaksgjennomforingSchema";
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

export function erArenaOpphavOgIngenEierskap(
  tiltaksgjennomforing: Tiltaksgjennomforing | undefined,
  migrerteTiltakstyper: string[],
) {
  return (
    tiltaksgjennomforing?.opphav === Opphav.ARENA &&
    !migrerteTiltakstyper?.includes(tiltaksgjennomforing.tiltakstype.arenaKode)
  );
}

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
  if (tiltaksgjennomforing?.arrangor?.id) {
    return tiltaksgjennomforing.arrangor.id;
  }

  if (avtale.arrangor.underenheter.length === 1) {
    return avtale.arrangor.underenheter[0].id;
  }

  return undefined;
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
      startDato: tiltaksgjennomforing
        ? tiltaksgjennomforing.startDato
        : defaultOppstartType(avtale) === TiltaksgjennomforingOppstartstype.LOPENDE
          ? avtale.startDato
          : undefined,
      sluttDato: tiltaksgjennomforing
        ? tiltaksgjennomforing.sluttDato
        : defaultOppstartType(avtale) === TiltaksgjennomforingOppstartstype.LOPENDE
          ? avtale.sluttDato
          : undefined,
    },
    arrangorId: defaultArrangor(avtale, tiltaksgjennomforing),
    oppstart: tiltaksgjennomforing?.oppstart || defaultOppstartType(avtale),
    apentForInnsok: tiltaksgjennomforing?.apentForInnsok ?? true,
    kontaktpersoner: tiltaksgjennomforing?.kontaktpersoner ?? [],
    stedForGjennomforing: tiltaksgjennomforing?.stedForGjennomforing ?? null,
    arrangorKontaktpersoner:
      tiltaksgjennomforing?.arrangor?.kontaktpersoner.map((p: ArrangorKontaktperson) => p.id) ?? [],
    beskrivelse: tiltaksgjennomforing?.beskrivelse ?? avtale.beskrivelse,
    faneinnhold: tiltaksgjennomforing?.faneinnhold ?? avtale.faneinnhold,
    opphav: tiltaksgjennomforing?.opphav ?? Opphav.MR_ADMIN_FLATE,
    deltidsprosent: tiltaksgjennomforing?.deltidsprosent ?? 100,
    visEstimertVentetid: !!tiltaksgjennomforing?.estimertVentetid?.enhet,
    estimertVentetid: tiltaksgjennomforing?.estimertVentetid ?? null,
    tilgjengeligForArrangorFraOgMedDato:
      tiltaksgjennomforing?.tilgjengeligForArrangorFraOgMedDato ?? null,
    amoKategorisering:
      tiltaksgjennomforing?.amoKategorisering ?? avtale.amoKategorisering ?? undefined,
  };
}
