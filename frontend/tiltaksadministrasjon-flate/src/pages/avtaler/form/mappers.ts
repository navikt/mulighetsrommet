import {
  AvtaleDetaljerOutputValues,
  AvtaleFormValues,
  PersonopplysningerOutputValues,
  PrismodellValues,
  VeilederinfoOutputValues,
} from "@/pages/avtaler/form/validation";
import {
  AmoKategoriseringRequest,
  DetaljerRequest,
  KurstypeKode,
  OpplaringKategoriseringDetaljer,
  OpprettAvtaleRequest,
  PersonvernRequest,
  PrismodellRequest,
  VeilederinfoRequest,
} from "@tiltaksadministrasjon/api-client";
import { v4 } from "uuid";

export function toOpprettAvtaleRequest(id: string, data: AvtaleFormValues): OpprettAvtaleRequest {
  return {
    id,
    detaljer: toDetaljerRequest(data),
    veilederinformasjon: toVeilederinfoRequest(data),
    personvern: toPersonvernRequest(data),
    prismodeller: toPrismodellRequest(data),
  };
}

export function toPrismodellRequest(data: PrismodellValues): PrismodellRequest[] {
  return data.prismodeller.map((prismodell) => ({
    ...prismodell,
    prisbetingelser: prismodell.prisbetingelser || null,
    id: prismodell.id ?? v4(),
    satser: prismodell.satser ?? [],
    tilsagnPerDeltaker: prismodell.tilsagnPerDeltaker,
  }));
}

export function toPersonvernRequest(data: PersonopplysningerOutputValues): PersonvernRequest {
  return {
    ...data.personvern,
    annetBeskrivelse: data.personvern.annetBeskrivelse ?? null,
  };
}

export function toDetaljerRequest(data: AvtaleDetaljerOutputValues): DetaljerRequest {
  const detaljer = data.detaljer;
  return {
    ...detaljer,
    sakarkivNummer: detaljer.sakarkivNummer || null,
    sluttDato: detaljer.sluttDato || null,
    arrangor: detaljer.arrangor?.hovedenhet ? { ...detaljer.arrangor } : null,
    amoKategorisering: detaljer.amoKategorisering || null,
    opsjonsmodell: {
      type: detaljer.opsjonsmodell.type,
      opsjonMaksVarighet: detaljer.opsjonsmodell.opsjonMaksVarighet || null,
      customOpsjonsmodellNavn: detaljer.opsjonsmodell.customOpsjonsmodellNavn || null,
    },
    utdanningslop: detaljer.utdanningslop || null,
  };
}

export function toVeilederinfoRequest(data: VeilederinfoOutputValues): VeilederinfoRequest {
  const veilederinformasjon = data.veilederinformasjon;
  return {
    beskrivelse: veilederinformasjon.beskrivelse,
    faneinnhold: veilederinformasjon.faneinnhold
      ? {
          forHvemInfoboks: veilederinformasjon.faneinnhold.forHvemInfoboks || null,
          forHvem: veilederinformasjon.faneinnhold.forHvem || null,
          detaljerOgInnholdInfoboks:
            veilederinformasjon.faneinnhold.detaljerOgInnholdInfoboks || null,
          detaljerOgInnhold: veilederinformasjon.faneinnhold.detaljerOgInnhold || null,
          pameldingOgVarighetInfoboks:
            veilederinformasjon.faneinnhold.pameldingOgVarighetInfoboks || null,
          pameldingOgVarighet: veilederinformasjon.faneinnhold.pameldingOgVarighet || null,
          kontaktinfo: veilederinformasjon.faneinnhold.kontaktinfo || null,
          kontaktinfoInfoboks: veilederinformasjon.faneinnhold.kontaktinfoInfoboks || null,
          lenker: veilederinformasjon.faneinnhold.lenker || null,
          oppskrift: veilederinformasjon.faneinnhold.oppskrift || null,
          delMedBruker: veilederinformasjon.faneinnhold.delMedBruker || null,
        }
      : null,
    navEnheter: veilederinformasjon.navRegioner
      .concat(veilederinformasjon.navKontorer)
      .concat(veilederinformasjon.navAndreEnheter),
  };
}

export function toAmoKategoriseringRequest(
  opplaring: OpplaringKategoriseringDetaljer | null,
): AmoKategoriseringRequest | null {
  switch (opplaring?.kurstype?.kode) {
    case KurstypeKode.BRANSJE_OG_YRKESRETTET:
      return {
        kurstype: KurstypeKode.BRANSJE_OG_YRKESRETTET,
        bransje: opplaring.bransje?.kode ?? null,
        sertifiseringer: opplaring.sertifiseringer,
        forerkort: opplaring.forerkort.map((f) => f.kode),
        innholdElementer: opplaring.innholdElementer.map((e) => e.kode),
        norskprove: null,
      };
    case KurstypeKode.NORSKOPPLAERING:
      return {
        kurstype: KurstypeKode.NORSKOPPLAERING,
        innholdElementer: opplaring.innholdElementer.map((e) => e.kode),
        norskprove: opplaring.norskprove,
        bransje: null,
        sertifiseringer: null,
        forerkort: null,
      };
    case KurstypeKode.GRUNNLEGGENDE_FERDIGHETER:
      return {
        kurstype: KurstypeKode.GRUNNLEGGENDE_FERDIGHETER,
        innholdElementer: opplaring.innholdElementer.map((e) => e.kode),
        norskprove: null,
        bransje: null,
        sertifiseringer: null,
        forerkort: null,
      };
    case KurstypeKode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE:
      return {
        kurstype: KurstypeKode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
        innholdElementer: opplaring.innholdElementer.map((e) => e.kode),
        norskprove: null,
        bransje: null,
        sertifiseringer: null,
        forerkort: null,
      };
    case KurstypeKode.STUDIESPESIALISERING:
      return {
        kurstype: KurstypeKode.STUDIESPESIALISERING,
        innholdElementer: null,
        norskprove: null,
        bransje: null,
        sertifiseringer: null,
        forerkort: null,
      };

    case undefined:
      return null;
  }
}
