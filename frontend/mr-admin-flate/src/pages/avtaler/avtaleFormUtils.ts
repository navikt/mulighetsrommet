import { AvtaleFormValues } from "@/schemas/avtale";
import { getUtdanningslop } from "@/schemas/avtaledetaljer";
import {
  AvtaleRequest,
  DetaljerRequest,
  PersonvernRequest,
  VeilederinfoRequest,
} from "@tiltaksadministrasjon/api-client";
import { v4 } from "uuid";

export interface RequestValues {
  data: AvtaleFormValues;
  id?: string;
}

export function toAvtaleRequest({ data, id }: RequestValues): AvtaleRequest {
  const { veilederinformasjon, satser, detaljer } = data;
  return {
    id: id ?? v4(),
    detaljer: {
      ...detaljer,
      sakarkivNummer: detaljer.sakarkivNummer || null,
      sluttDato: detaljer.sluttDato || null,
      arrangor:
        detaljer.arrangorHovedenhet && detaljer.arrangorUnderenheter
          ? {
              hovedenhet: detaljer.arrangorHovedenhet,
              underenheter: detaljer.arrangorUnderenheter,
              kontaktpersoner: detaljer.arrangorKontaktpersoner || [],
            }
          : null,
      amoKategorisering: detaljer.amoKategorisering || null,
      opsjonsmodell: {
        type: detaljer.opsjonsmodell.type,
        opsjonMaksVarighet: detaljer.opsjonsmodell.opsjonMaksVarighet || null,
        customOpsjonsmodellNavn: detaljer.opsjonsmodell.customOpsjonsmodellNavn || null,
      },
      utdanningslop: getUtdanningslop(data),
    },
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
    personvern: data.personvern,
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

export function toDetaljerRequest({ data }: RequestValues): DetaljerRequest {
  const detaljer = data.detaljer;
  return {
    ...detaljer,
    sakarkivNummer: detaljer.sakarkivNummer || null,
    sluttDato: detaljer.sluttDato || null,
    arrangor:
      detaljer.arrangorHovedenhet && detaljer.arrangorUnderenheter
        ? {
            hovedenhet: detaljer.arrangorHovedenhet,
            underenheter: detaljer.arrangorUnderenheter,
            kontaktpersoner: detaljer.arrangorKontaktpersoner || [],
          }
        : null,
    amoKategorisering: detaljer.amoKategorisering || null,
    opsjonsmodell: {
      type: detaljer.opsjonsmodell.type,
      opsjonMaksVarighet: detaljer.opsjonsmodell.opsjonMaksVarighet || null,
      customOpsjonsmodellNavn: detaljer.opsjonsmodell.customOpsjonsmodellNavn || null,
    },
    utdanningslop: getUtdanningslop(data),
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
