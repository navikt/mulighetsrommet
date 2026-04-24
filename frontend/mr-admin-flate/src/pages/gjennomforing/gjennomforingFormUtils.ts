import {
  AmoKategoriseringRequest,
  AvtaleDto,
  Faneinnhold,
  GjennomforingDetaljerRequest,
  GjennomforingRequest,
  GjennomforingVeilederinfoRequest,
  UtdanningslopDbo,
} from "@tiltaksadministrasjon/api-client";
import {
  GjennomforingDetaljerOutputValues,
  GjennomforingFormValues,
  GjennomforingVeilederinfoOutputValues,
} from "@/schemas/gjennomforing";

export function toCreateGjennomforingRequest(
  id: string,
  data: GjennomforingFormValues,
  avtale: AvtaleDto,
): GjennomforingRequest {
  return {
    id,
    tiltakstypeId: avtale.tiltakstype.id,
    avtaleId: avtale.id,
    detaljer: toGjennomforingDetaljerRequest(data),
    veilederinformasjon: toGjennomforingVeilederinfoRequest(data),
  };
}

export function toGjennomforingDetaljerRequest(
  data: GjennomforingDetaljerOutputValues,
): GjennomforingDetaljerRequest {
  return {
    navn: data.navn,
    startDato: data.startDato,
    sluttDato: data.sluttDato || null,
    antallPlasser: data.antallPlasser,
    arrangorId: data.arrangorId,
    arrangorKontaktpersoner: data.arrangorKontaktpersoner,
    administratorer: data.administratorer,
    oppstart: data.oppstart,
    pameldingType: data.pameldingType,
    oppmoteSted: data.oppmoteSted || null,
    deltidsprosent: data.deltidsprosent,
    estimertVentetid: data.estimertVentetid || null,
    tilgjengeligForArrangorDato: data.tilgjengeligForArrangorDato || null,
    amoKategorisering: data.amoKategorisering as AmoKategoriseringRequest | null,
    utdanningslop: data.utdanningslop as UtdanningslopDbo | null,
    prismodellId: data.prismodellId || null,
  };
}

export function toGjennomforingVeilederinfoRequest(
  data: GjennomforingVeilederinfoOutputValues,
): GjennomforingVeilederinfoRequest {
  return {
    navRegioner: data.veilederinformasjon.navRegioner,
    navKontorer: data.veilederinformasjon.navKontorer,
    navAndreEnheter: data.veilederinformasjon.navAndreEnheter,
    beskrivelse: data.veilederinformasjon.beskrivelse ?? null,
    faneinnhold: (data.veilederinformasjon.faneinnhold as Faneinnhold | null) ?? null,
    kontaktpersoner: data.veilederinformasjon.kontaktpersoner
      .filter((k) => k.navIdent !== "")
      .map((k) => ({ navIdent: k.navIdent, beskrivelse: k.beskrivelse || null })),
  };
}
