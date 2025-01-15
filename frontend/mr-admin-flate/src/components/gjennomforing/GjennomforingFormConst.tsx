import {
  ArrangorKontaktperson,
  AvtaleDto,
  NavAnsatt,
  Opphav,
  GjennomforingDto,
  GjennomforingOppstartstype,
  Utdanningslop,
  UtdanningslopDbo,
} from "@mr/api-client";
import { InferredGjennomforingSchema } from "@/components/redaksjoneltInnhold/GjennomforingSchema";
import { DeepPartial } from "react-hook-form";
import { isKursTiltak } from "@mr/frontend-common/utils/utils";

export function defaultOppstartType(avtale?: AvtaleDto): GjennomforingOppstartstype {
  if (!avtale) {
    return GjennomforingOppstartstype.LOPENDE;
  }

  const tiltakskode = avtale.tiltakstype.tiltakskode;
  return isKursTiltak(tiltakskode)
    ? GjennomforingOppstartstype.FELLES
    : GjennomforingOppstartstype.LOPENDE;
}

function defaultNavRegion(
  avtale: AvtaleDto,
  tiltaksgjennomforing?: GjennomforingDto,
): string | undefined {
  if (tiltaksgjennomforing?.navRegion) {
    return tiltaksgjennomforing.navRegion.enhetsnummer;
  }
  if (avtale.kontorstruktur.length === 1) {
    return avtale.kontorstruktur[0].region.enhetsnummer;
  }
}

function defaultNavEnheter(avtale: AvtaleDto, tiltaksgjennomforing?: GjennomforingDto): string[] {
  if (tiltaksgjennomforing?.navEnheter) {
    return tiltaksgjennomforing.navEnheter.map((enhet) => enhet.enhetsnummer);
  }
  if (avtale.kontorstruktur.length === 1) {
    return avtale.kontorstruktur[0].kontorer.map((enhet) => enhet.enhetsnummer);
  }
  return [];
}

function defaultArrangor(
  avtale: AvtaleDto,
  tiltaksgjennomforing?: GjennomforingDto,
): string | undefined {
  if (tiltaksgjennomforing?.arrangor?.id) {
    return tiltaksgjennomforing.arrangor.id;
  }

  if (avtale.arrangor.underenheter.length === 1) {
    return avtale.arrangor.underenheter[0].id;
  }

  return undefined;
}

export function defaultGjennomforingData(
  ansatt: NavAnsatt,
  avtale: AvtaleDto,
  tiltaksgjennomforing?: GjennomforingDto,
): DeepPartial<InferredGjennomforingSchema> {
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
        : defaultOppstartType(avtale) === GjennomforingOppstartstype.LOPENDE
          ? avtale.startDato
          : undefined,
      sluttDato: tiltaksgjennomforing
        ? tiltaksgjennomforing.sluttDato
        : defaultOppstartType(avtale) === GjennomforingOppstartstype.LOPENDE
          ? avtale.sluttDato
          : undefined,
    },
    arrangorId: defaultArrangor(avtale, tiltaksgjennomforing),
    oppstart: tiltaksgjennomforing?.oppstart || defaultOppstartType(avtale),
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
    utdanningslop: tiltaksgjennomforing?.utdanningslop
      ? toUtdanningslopDbo(tiltaksgjennomforing.utdanningslop)
      : avtale.utdanningslop
        ? toUtdanningslopDbo(avtale.utdanningslop)
        : undefined,
  };
}

function toUtdanningslopDbo(data: Utdanningslop): UtdanningslopDbo {
  return {
    utdanningsprogram: data.utdanningsprogram.id,
    utdanninger: data.utdanninger.map((utdanning) => utdanning.id),
  };
}
