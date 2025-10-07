import { AvtaleFormValues } from "@/schemas/avtale";
import { getUtdanningslop } from "@/schemas/avtaledetaljer";
import { AvtaleDto, AvtaleRequest, PersonvernRequest } from "@tiltaksadministrasjon/api-client";
import { v4 } from "uuid";

export function toAvtaleRequest(data: AvtaleFormValues, avtale?: AvtaleDto): AvtaleRequest {
  const { navn, startDato, beskrivelse, avtaletype, satser, administratorer } = data;
  return {
    id: avtale?.id ?? v4(),
    navn,
    administratorer,
    beskrivelse,
    avtaletype,
    startDato,
    sakarkivNummer: data.sakarkivNummer || null,
    sluttDato: data.sluttDato || null,
    navEnheter: data.navRegioner.concat(data.navKontorer).concat(data.navEnheterAndre),
    avtalenummer: avtale?.avtalenummer ?? null,
    faneinnhold: data.faneinnhold
      ? {
          forHvemInfoboks: data.faneinnhold.forHvemInfoboks || null,
          forHvem: data.faneinnhold.forHvem || null,
          detaljerOgInnholdInfoboks: data.faneinnhold.detaljerOgInnholdInfoboks || null,
          detaljerOgInnhold: data.faneinnhold.detaljerOgInnhold || null,
          pameldingOgVarighetInfoboks: data.faneinnhold.pameldingOgVarighetInfoboks || null,
          pameldingOgVarighet: data.faneinnhold.pameldingOgVarighet || null,
          kontaktinfo: data.faneinnhold.kontaktinfo || null,
          kontaktinfoInfoboks: data.faneinnhold.kontaktinfoInfoboks || null,
          lenker: data.faneinnhold.lenker || null,
          oppskrift: data.faneinnhold.oppskrift || null,
          delMedBruker: data.faneinnhold.delMedBruker || null,
        }
      : null,
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

export function toPersonvernRequest(
  values: AvtaleFormValues,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  avtale: AvtaleDto,
): PersonvernRequest {
  return {
    ...values.personvern,
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
