import {
  AvtaleFormValues,
  PersonvernValues,
  PrismodellValues,
  VeilederinformasjonValues,
} from "@/schemas/avtale";
import { AvtaleDetaljerValues, getUtdanningslop } from "@/schemas/avtaledetaljer";
import {
  DetaljerRequest,
  OpprettAvtaleRequest,
  PersonvernRequest,
  PrismodellRequest,
  VeilederinfoRequest,
} from "@tiltaksadministrasjon/api-client";
import { v4 } from "uuid";

export interface RequestValues {
  data: AvtaleFormValues;
  id?: string;
}

export function toOpprettAvtaleRequest(data: AvtaleFormValues): OpprettAvtaleRequest {
  return {
    id: v4(),
    detaljer: toDetaljerRequest({ data: data }),
    veilederinformasjon: toVeilederinfoRequest({ data: data }),
    personvern: toPersonvernRequest({ data: data }),
    prismodell: toPrismodellRequest({ data: data }),
  };
}

export function toPrismodellRequest({ data }: { data: PrismodellValues }): PrismodellRequest {
  return {
    ...data.prismodell,
    id: data.prismodell.id ?? v4(),
  };
}

export function toPersonvernRequest({ data }: { data: PersonvernValues }): PersonvernRequest {
  return {
    ...data.personvern,
  };
}

export function toDetaljerRequest({ data }: { data: AvtaleDetaljerValues }): DetaljerRequest {
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
    utdanningslop: getUtdanningslop(data),
  };
}

export function toVeilederinfoRequest({
  data,
}: {
  data: VeilederinformasjonValues;
}): VeilederinfoRequest {
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
