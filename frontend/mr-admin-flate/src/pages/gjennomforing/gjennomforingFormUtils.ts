import {
  AmoKategoriseringRequest,
  AvtaleDto,
  Faneinnhold,
  GjennomforingDetaljerRequest,
  GjennomforingDetaljerRequestEstimertVentetid,
  GjennomforingRequest,
  GjennomforingVeilederinfoRequest,
  UtdanningslopDbo,
} from "@tiltaksadministrasjon/api-client";
import { DeepPartial } from "react-hook-form";

export type GjennomforingFormValues = GjennomforingDetaljerRequest & {
  veilederinformasjon: GjennomforingVeilederinfoRequest;
};

export function toCreateGjennomforingRequest(
  id: string,
  data: DeepPartial<GjennomforingFormValues>,
  avtale: AvtaleDto,
): GjennomforingRequest {
  return {
    id,
    tiltakstypeId: avtale.tiltakstype.id,
    avtaleId: avtale.id,
    detaljer: {
      navn: data.navn ?? "",
      startDato: data.startDato ?? null,
      sluttDato: data.sluttDato || null,
      antallPlasser: data.antallPlasser ?? null,
      arrangorId: data.arrangorId ?? null,
      arrangorKontaktpersoner: (data.arrangorKontaktpersoner ?? []).filter(
        (k): k is string => k !== undefined,
      ),
      kontaktpersoner: (data.kontaktpersoner ?? [])
        .filter((k) => k !== undefined && k.navIdent !== "")
        .map((k) => ({ navIdent: k?.navIdent ?? "", beskrivelse: k?.beskrivelse ?? null })),
      administratorer: (data.administratorer ?? []).filter((k): k is string => k !== undefined),
      oppstart: data.oppstart ?? null,
      oppmoteSted: data.oppmoteSted ?? null,
      deltidsprosent: data.deltidsprosent ?? 100,
      estimertVentetid: data.estimertVentetid
        ? (data.estimertVentetid as GjennomforingDetaljerRequestEstimertVentetid)
        : null,
      tilgjengeligForArrangorDato: data.tilgjengeligForArrangorDato ?? null,
      amoKategorisering: (data.amoKategorisering as AmoKategoriseringRequest | null) ?? null,
      utdanningslop: (data.utdanningslop as UtdanningslopDbo | null) ?? null,
      pameldingType: data.pameldingType ?? null,
      prismodellId: data.prismodellId || null,
    },
    veilederinformasjon: {
      navRegioner: (data.veilederinformasjon?.navRegioner ?? []).filter(
        (k): k is string => k !== undefined,
      ),
      navKontorer: (data.veilederinformasjon?.navKontorer ?? []).filter(
        (k): k is string => k !== undefined,
      ),
      navAndreEnheter: (data.veilederinformasjon?.navAndreEnheter ?? []).filter(
        (k): k is string => k !== undefined,
      ),
      beskrivelse: data.veilederinformasjon?.beskrivelse ?? null,
      faneinnhold: (data.veilederinformasjon?.faneinnhold as Faneinnhold | null) ?? null,
    },
  };
}

export function toGjennomforingDetaljerRequest(
  data: GjennomforingFormValues,
): GjennomforingDetaljerRequest {
  return {
    navn: data.navn,
    startDato: data.startDato,
    sluttDato: data.sluttDato || null,
    antallPlasser: data.antallPlasser,
    arrangorId: data.arrangorId,
    arrangorKontaktpersoner: data.arrangorKontaktpersoner,
    kontaktpersoner: data.kontaktpersoner.map((k) => ({
      navIdent: k.navIdent,
      beskrivelse: k.beskrivelse ?? null,
    })),
    administratorer: data.administratorer,
    oppstart: data.oppstart,
    oppmoteSted: data.oppmoteSted,
    deltidsprosent: data.deltidsprosent,
    estimertVentetid: data.estimertVentetid ?? null,
    tilgjengeligForArrangorDato: data.tilgjengeligForArrangorDato ?? null,
    amoKategorisering: data.amoKategorisering ?? null,
    utdanningslop: data.utdanningslop ?? null,
    prismodellId: data.prismodellId || null,
    pameldingType: data.pameldingType,
  };
}

export function toGjennomforingVeilederinfoRequest(
  data: GjennomforingFormValues,
): GjennomforingVeilederinfoRequest {
  return {
    navRegioner: data.veilederinformasjon.navRegioner,
    navKontorer: data.veilederinformasjon.navKontorer,
    navAndreEnheter: data.veilederinformasjon.navAndreEnheter,
    beskrivelse: data.veilederinformasjon.beskrivelse,
    faneinnhold: data.veilederinformasjon.faneinnhold,
  };
}
