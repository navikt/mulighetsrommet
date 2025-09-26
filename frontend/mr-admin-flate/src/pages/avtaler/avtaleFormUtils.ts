import { ApiMutationResult } from "@/hooks/useApiMutation";
import { AvtaleFormValues } from "@/schemas/avtale";
import { getUtdanningslop } from "@/schemas/avtaledetaljer";
import {
  AvtaleRequest,
  ProblemDetail,
  ValidationError as LegacyValidationError,
} from "@mr/api-client-v2";
import { AvtaleDto, ValidationError } from "@tiltaksadministrasjon/api-client";
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
  onValidationError: (e: ValidationError | LegacyValidationError) => void;
  onSuccess: (dto: { data: AvtaleDto }) => void;
}) {
  const { detaljer, prismodell, personvern, veilederinformasjon } = data;
  const requestBody: AvtaleRequest = {
    id: avtale?.id ?? v4(),
    detaljer: {
      navn: detaljer.navn,
      administratorer: detaljer.administratorer,
      avtaletype: detaljer.avtaletype,
      startDato: detaljer.startDato,
      sakarkivNummer: detaljer.sakarkivNummer || null,
      sluttDato: detaljer.sluttDato || null,

      avtalenummer: avtale?.avtalenummer ?? null,
      arrangor: detaljer.arrangor,
      tiltakskode: detaljer.tiltakskode,
      amoKategorisering: detaljer.amoKategorisering || null,
      opsjonsmodell: detaljer.opsjonsmodell,
      utdanningslop: getUtdanningslop(detaljer),
    },
    prismodell: prismodell,
    personvern: {
      personopplysninger: personvern.personopplysninger,
      personvernBekreftet: personvern.personvernBekreftet,
    },
    veilederinformasjon: {
      navEnheter: veilederinformasjon.navRegioner
        .concat(veilederinformasjon.navKontorer)
        .concat(veilederinformasjon.navAndreEnheter),
      beskrivelse: veilederinformasjon.redaksjoneltInnhold.beskrivelse,
      faneinnhold: veilederinformasjon.redaksjoneltInnhold.faneinnhold,
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
