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

export type GjennomforingFormValues = GjennomforingDetaljerRequest & {
  veilederinformasjon: GjennomforingVeilederinfoRequest;
};

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

export function toGjennomforingDetaljerRequest(data: GjennomforingFormValues) {
  return {
    navn: data.navn,
    startDato: data.startDato,
    sluttDato: data.sluttDato || null,
    antallPlasser: data.antallPlasser,
    arrangorId: data.arrangorId ?? null,
    arrangorKontaktpersoner: data.arrangorKontaktpersoner,
    kontaktpersoner: data.kontaktpersoner
      .filter((k) => k.navIdent !== "")
      .map((k) => ({ navIdent: k.navIdent, beskrivelse: k.beskrivelse ?? null })),
    administratorer: data.administratorer,
    oppstart: data.oppstart ?? null,
    oppmoteSted: data.oppmoteSted ?? null,
    deltidsprosent: data.deltidsprosent,
    estimertVentetid: data.estimertVentetid
      ? (data.estimertVentetid as GjennomforingDetaljerRequestEstimertVentetid)
      : null,
    tilgjengeligForArrangorDato: data.tilgjengeligForArrangorDato ?? null,
    amoKategorisering: (data.amoKategorisering as AmoKategoriseringRequest | null) ?? null,
    utdanningslop: (data.utdanningslop as UtdanningslopDbo | null) ?? null,
    pameldingType: data.pameldingType ?? null,
    prismodellId: data.prismodellId || null,
  };
}

export function toGjennomforingVeilederinfoRequest(
  data: GjennomforingFormValues,
): GjennomforingVeilederinfoRequest {
  return {
    navRegioner: data.veilederinformasjon.navRegioner,
    navKontorer: data.veilederinformasjon.navKontorer,
    navAndreEnheter: data.veilederinformasjon.navAndreEnheter,
    beskrivelse: data.veilederinformasjon.beskrivelse ?? null,
    faneinnhold: (data.veilederinformasjon.faneinnhold as Faneinnhold | null) ?? null,
    kontaktpersoner: data.kontaktpersoner
      .filter((k) => k.navIdent !== "")
      .map((k) => ({ navIdent: k.navIdent, beskrivelse: k.beskrivelse ?? null })),
  };
}
