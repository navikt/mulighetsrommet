import {
  AvtaleDetaljerOutputValues,
  AvtaleFormValues,
  PersonopplysningerOutputValues,
  PrismodellValues,
  VeilederinfoOutputValues,
} from "@/pages/avtaler/form/validation";
import {
  AmoKategoriseringDto,
  AmoKategoriseringRequest,
  AmoKurstype,
  DetaljerRequest,
  OpprettAvtaleRequest,
  PersonvernRequest,
  PrismodellRequest,
  VeilederinfoRequest,
} from "@tiltaksadministrasjon/api-client";
import { v4 } from "uuid";

export function toOpprettAvtaleRequest(id: string, data: AvtaleFormValues): OpprettAvtaleRequest {
  return {
    id,
    detaljer: toDetaljerRequest({ data: data }),
    veilederinformasjon: toVeilederinfoRequest(data),
    personvern: toPersonvernRequest({ data: data }),
    prismodeller: toPrismodellRequest({ data: data }),
  };
}

export function toPrismodellRequest({ data }: { data: PrismodellValues }): PrismodellRequest[] {
  return data.prismodeller.map((prismodell) => ({
    ...prismodell,
    id: prismodell.id ?? v4(),
    satser: prismodell.satser ?? [],
    tilsagnPerDeltaker: prismodell.tilsagnPerDeltaker,
  }));
}

export function toPersonvernRequest({
  data,
}: {
  data: PersonopplysningerOutputValues;
}): PersonvernRequest {
  return {
    ...data.personvern,
  };
}

export function toDetaljerRequest({ data }: { data: AvtaleDetaljerOutputValues }): DetaljerRequest {
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
  amoKategorisering: AmoKategoriseringDto | null,
): AmoKategoriseringRequest | null {
  switch (amoKategorisering?.kurstype) {
    case AmoKurstype.BRANSJE_OG_YRKESRETTET:
      return {
        kurstype: AmoKurstype.BRANSJE_OG_YRKESRETTET,
        bransje: amoKategorisering.bransje,
        sertifiseringer: amoKategorisering.sertifiseringer,
        forerkort: amoKategorisering.forerkort,
        innholdElementer: amoKategorisering.innholdElementer,
        norskprove: null,
      };
    case AmoKurstype.NORSKOPPLAERING:
      return {
        kurstype: AmoKurstype.NORSKOPPLAERING,
        innholdElementer: amoKategorisering.innholdElementer,
        norskprove: amoKategorisering.norskprove,
        bransje: null,
        sertifiseringer: null,
        forerkort: null,
      };
    case AmoKurstype.GRUNNLEGGENDE_FERDIGHETER:
      return {
        kurstype: AmoKurstype.GRUNNLEGGENDE_FERDIGHETER,
        innholdElementer: amoKategorisering.innholdElementer,
        norskprove: null,
        bransje: null,
        sertifiseringer: null,
        forerkort: null,
      };
    case AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE:
      return {
        kurstype: AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
        innholdElementer: amoKategorisering.innholdElementer,
        norskprove: null,
        bransje: null,
        sertifiseringer: null,
        forerkort: null,
      };
    case AmoKurstype.STUDIESPESIALISERING:
      return {
        kurstype: AmoKurstype.STUDIESPESIALISERING,
        innholdElementer: null,
        norskprove: null,
        bransje: null,
        sertifiseringer: null,
        forerkort: null,
      };

    case null:
    case undefined:
      return null;
  }
}
