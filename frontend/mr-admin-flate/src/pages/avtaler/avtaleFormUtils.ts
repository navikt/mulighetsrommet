import { AvtaleFormValues } from "@/schemas/avtale";
import { getUtdanningslop } from "@/schemas/avtaledetaljer";
import {
  AvtaleRequest,
  PersonvernRequest,
  VeilederinfoRequest,
} from "@tiltaksadministrasjon/api-client";
import { v4 } from "uuid";

export interface RequestValues {
  data: AvtaleFormValues;
  id?: string;
}

export function toAvtaleRequest({ data, id }: RequestValues): AvtaleRequest {
  const { navn, startDato, veilederinformasjon, avtaletype, satser, administratorer } = data;
  return {
    id: id ?? v4(),
    navn,
    administratorer,
    avtaletype,
    startDato,
    sakarkivNummer: data.sakarkivNummer || null,
    sluttDato: data.sluttDato || null,
    veilederinformasjon: {
      navEnheter: veilederinformasjon.navRegioner
        .concat(veilederinformasjon.navKontorer)
        .concat(veilederinformasjon.navAndreEnheter),
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
      beskrivelse: veilederinformasjon.beskrivelse,
    },
    arrangor:
      data.arrangorHovedenhet && data.arrangorUnderenheter
        ? {
            hovedenhet: data.arrangorHovedenhet,
            underenheter: data.arrangorUnderenheter,
            kontaktpersoner: data.arrangorKontaktpersoner || [],
          }
        : null,
    tiltakskode: data.tiltakskode,
    personvern: data.personvern,
    amoKategorisering: data.amoKategorisering || null,
    opsjonsmodell: {
      type: data.opsjonsmodell.type,
      opsjonMaksVarighet: data.opsjonsmodell.opsjonMaksVarighet || null,
      customOpsjonsmodellNavn: data.opsjonsmodell.customOpsjonsmodellNavn || null,
    },
    utdanningslop: getUtdanningslop(data),
    prismodell: {
      type: data.prismodell,
      prisbetingelser: data.prisbetingelser || null,
      satser,
    },
  };
}

export function toPersonvernRequest({ data }: RequestValues): PersonvernRequest {
  return {
    ...data.personvern,
  };
}

export function toVeilederinfoRequest({ data }: RequestValues): VeilederinfoRequest {
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

export function mapNameToSchemaPropertyName(name: string) {
  const mapping: { [name: string]: string } = {
    opsjonsmodell: "opsjonsmodell.type",
    opsjonMaksVarighet: "opsjonsmodell.opsjonMaksVarighet",
    customOpsjonsmodellNavn: "opsjonsmodell.customOpsjonsmodellNavn",
    tiltakstypeId: "tiltakskode",
    utdanningslop: "utdanningslop.utdanninger",
  };
  return (mapping[name] ?? name) as keyof AvtaleFormValues;
}
