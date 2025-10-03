import { ApiMutationResult } from "@/hooks/useApiMutation";
import { AvtaleFormValues } from "@/schemas/avtale";
import { getUtdanningslop } from "@/schemas/avtaledetaljer";
import {
  AvtaleDto,
  AvtaleRequest,
  ProblemDetail,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { v4 } from "uuid";

export async function onSubmitAvtaleForm({
  avtale,
  data,
  mutation,
  onValidationError,
  onSuccess,
}: {
  avtale?: AvtaleDto;
  data: AvtaleFormValues;
  mutation: ApiMutationResult<{ data: AvtaleDto }, ProblemDetail, AvtaleRequest, unknown>;
  onValidationError: (e: ValidationError) => void;
  onSuccess: (dto: { data: AvtaleDto }) => void;
}) {
  const {
    navn,
    startDato,
    beskrivelse,
    avtaletype,
    faneinnhold,
    personopplysninger,
    personvernBekreftet,
    satser,
    administratorer,
  } = data;
  const requestBody: AvtaleRequest = {
    id: avtale?.id ?? v4(),
    navn,
    administratorer,
    beskrivelse,
    faneinnhold,
    personopplysninger,
    personvernBekreftet,
    avtaletype,
    startDato,
    sakarkivNummer: data.sakarkivNummer || null,
    sluttDato: data.sluttDato || null,
    navEnheter: data.navRegioner.concat(data.navKontorer).concat(data.navEnheterAndre),
    avtalenummer: avtale?.avtalenummer ?? null,
    arrangor:
      data.arrangorHovedenhet && data.arrangorUnderenheter
        ? {
            hovedenhet: data.arrangorHovedenhet,
            underenheter: data.arrangorUnderenheter,
            kontaktpersoner: data.arrangorKontaktpersoner || [],
          }
        : null,
    tiltakskode: data.tiltakskode,
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

  mutation.mutate(requestBody, {
    onSuccess,
    onValidationError,
  });
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
