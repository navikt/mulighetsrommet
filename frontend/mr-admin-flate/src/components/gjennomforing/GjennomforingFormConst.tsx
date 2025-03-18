import {
  ArrangorKontaktperson,
  AvtaleDto,
  NavAnsatt,
  Opphav,
  GjennomforingDto,
  GjennomforingOppstartstype,
  Utdanningslop,
  UtdanningslopDbo,
} from "@mr/api-client-v2";
import { InferredGjennomforingSchema } from "@/components/redaksjoneltInnhold/GjennomforingSchema";
import { DeepPartial } from "react-hook-form";
import { isKursTiltak } from "@/utils/Utils";

export function defaultOppstartType(avtale?: AvtaleDto): GjennomforingOppstartstype {
  if (!avtale) {
    return GjennomforingOppstartstype.LOPENDE;
  }

  const tiltakskode = avtale.tiltakstype.tiltakskode;
  return isKursTiltak(tiltakskode)
    ? GjennomforingOppstartstype.FELLES
    : GjennomforingOppstartstype.LOPENDE;
}

function defaultNavRegion(avtale: AvtaleDto, gjennomforing?: GjennomforingDto): string | undefined {
  if (gjennomforing?.navRegion) {
    return gjennomforing.navRegion.enhetsnummer;
  }
  if (avtale.kontorstruktur.length === 1) {
    return avtale.kontorstruktur[0].region.enhetsnummer;
  }
}

function defaultNavEnheter(avtale: AvtaleDto, gjennomforing?: GjennomforingDto): string[] {
  if (gjennomforing?.navEnheter) {
    return gjennomforing.navEnheter.map((enhet) => enhet.enhetsnummer);
  }
  if (avtale.kontorstruktur.length === 1) {
    return avtale.kontorstruktur[0].kontorer.map((enhet) => enhet.enhetsnummer);
  }
  return [];
}

function defaultArrangor(avtale: AvtaleDto, gjennomforing?: GjennomforingDto): string | undefined {
  if (gjennomforing?.arrangor?.id) {
    return gjennomforing.arrangor.id;
  }

  if (avtale.arrangor?.underenheter.length === 1) {
    return avtale.arrangor.underenheter[0].id;
  }

  return undefined;
}

export function defaultGjennomforingData(
  ansatt: NavAnsatt,
  avtale: AvtaleDto,
  gjennomforing?: GjennomforingDto,
): DeepPartial<InferredGjennomforingSchema> {
  return {
    navn: gjennomforing?.navn || avtale.navn,
    avtaleId: avtale.id,
    navRegion: defaultNavRegion(avtale, gjennomforing),
    navEnheter: defaultNavEnheter(avtale, gjennomforing),
    administratorer: gjennomforing?.administratorer?.map((admin) => admin.navIdent) || [
      ansatt.navIdent,
    ],
    antallPlasser: gjennomforing?.antallPlasser,
    startOgSluttDato: {
      startDato: gjennomforing
        ? gjennomforing.startDato
        : defaultOppstartType(avtale) === GjennomforingOppstartstype.LOPENDE
          ? avtale.startDato
          : undefined,
      sluttDato: gjennomforing
        ? gjennomforing.sluttDato
        : defaultOppstartType(avtale) === GjennomforingOppstartstype.LOPENDE
          ? avtale.sluttDato
          : undefined,
    },
    arrangorId: defaultArrangor(avtale, gjennomforing),
    oppstart: gjennomforing?.oppstart || defaultOppstartType(avtale),
    kontaktpersoner: gjennomforing?.kontaktpersoner ?? [],
    stedForGjennomforing: gjennomforing?.stedForGjennomforing ?? null,
    arrangorKontaktpersoner:
      gjennomforing?.arrangor?.kontaktpersoner.map((p: ArrangorKontaktperson) => p.id) ?? [],
    beskrivelse: gjennomforing?.beskrivelse ?? avtale.beskrivelse,
    faneinnhold: gjennomforing?.faneinnhold ?? avtale.faneinnhold,
    opphav: gjennomforing?.opphav ?? Opphav.MR_ADMIN_FLATE,
    deltidsprosent: gjennomforing?.deltidsprosent ?? 100,
    visEstimertVentetid: !!gjennomforing?.estimertVentetid?.enhet,
    estimertVentetid: gjennomforing?.estimertVentetid ?? null,
    tilgjengeligForArrangorFraOgMedDato: gjennomforing?.tilgjengeligForArrangorFraOgMedDato ?? null,
    amoKategorisering: gjennomforing?.amoKategorisering ?? avtale.amoKategorisering ?? undefined,
    utdanningslop: gjennomforing?.utdanningslop
      ? toUtdanningslopDbo(gjennomforing.utdanningslop)
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
