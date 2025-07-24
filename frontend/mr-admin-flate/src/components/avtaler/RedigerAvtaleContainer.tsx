import { useUpsertAvtale } from "@/api/avtaler/useUpsertAvtale";
import { AvtaleFormValues, AvtaleFormInput, avtaleFormSchema } from "@/schemas/avtale";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  AvtaleDto,
  Prismodell,
  Tiltakskode,
  UtdanningslopDbo,
  ValidationError,
} from "@mr/api-client-v2";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { inneholderUrl } from "@/utils/Utils";
import { useNavigate } from "react-router";
import { useCallback } from "react";
import { DeepPartial, FormProvider, useForm } from "react-hook-form";
import { Separator } from "../detaljside/Metadata";
import { AvtaleFormKnapperad } from "./AvtaleFormKnapperad";

interface Props {
  onSuccess: (id: string) => void;
  avtale: AvtaleDto;
  defaultValues: DeepPartial<AvtaleFormValues>;
  children: React.ReactNode;
}

export function RedigerAvtaleContainer({ onSuccess, avtale, defaultValues, children }: Props) {
  const mutation = useUpsertAvtale();
  const navigate = useNavigate();
  const methods = useForm<AvtaleFormInput, any, AvtaleFormValues>({
    resolver: zodResolver(avtaleFormSchema),
    defaultValues,
  });
  const redigeringsModus = avtale ? inneholderUrl(avtale.id) : false;

  const postData = async (data: AvtaleFormValues) => {
    const requestBody = {
      ...data,
      id: avtale.id,
      navEnheter: data.navRegioner.concat(data.navKontorer).concat(data.navAndreEnheter),
      avtalenummer: avtale.avtalenummer || null,
      arrangor:
        data.arrangorHovedenhet && data.arrangorUnderenheter
          ? {
              hovedenhet: data.arrangorHovedenhet,
              underenheter: data.arrangorUnderenheter,
              kontaktpersoner: data.arrangorKontaktpersoner || [],
            }
          : null,
      tiltakstypeId: data.tiltakstype.id,
      prisbetingelser:
        !data.prismodell || data.prismodell === Prismodell.ANNEN_AVTALT_PRIS
          ? data.prisbetingelser || null
          : null,
      amoKategorisering: data.amoKategorisering || null,
      opsjonsmodell: {
        type: data.opsjonsmodell.type,
        opsjonMaksVarighet: data.opsjonsmodell.opsjonMaksVarighet || null,
        customOpsjonsmodellNavn: data.opsjonsmodell.customOpsjonsmodellNavn || null,
      },
      utdanningslop: getUtdanningslop(data),
      prismodell: data.prismodell ?? Prismodell.ANNEN_AVTALT_PRIS,
    };

    mutation.mutate(requestBody, {
      onSuccess: handleSuccess,
      onValidationError: (error: ValidationError) => {
        handleValidationError(error);
      },
    });
  };

  const handleSuccess = useCallback(
    (dto: { data: AvtaleDto }) => onSuccess(dto.data.id),
    [onSuccess],
  );
  const handleValidationError = useCallback(
    (validation: ValidationError) => {
      validation.errors.forEach((error) => {
        const name = mapNameToSchemaPropertyName(jsonPointerToFieldPath(error.pointer));
        methods.setError(name, { type: "custom", message: error.detail });
      });

      function mapNameToSchemaPropertyName(name: string) {
        const mapping: { [name: string]: string } = {
          opsjonsmodell: "opsjonsmodell.type",
          opsjonMaksVarighet: "opsjonsmodellD.opsjonMaksVarighet",
          customOpsjonsmodellNavn: "opsjonsmodell.customOpsjonsmodellNavn",
          tiltakstypeId: "tiltakstype",
          utdanningslop: "utdanningslop.utdanninger",
        };
        return (mapping[name] ?? name) as keyof AvtaleFormValues;
      }
    },
    [methods],
  );

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(postData)}>
        <AvtaleFormKnapperad redigeringsModus={redigeringsModus} onClose={() => navigate(-1)} />
        <Separator />
        {children}
      </form>
    </FormProvider>
  );
}

/**
 * Så lenge det mangler validering av utdanningsløp i frontend så trenger vi litt ekstra sanitering av data
 */
function getUtdanningslop(data: AvtaleFormValues): UtdanningslopDbo | null {
  if (data.tiltakstype.tiltakskode !== Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING) {
    return null;
  }

  if (!data.utdanningslop?.utdanningsprogram || !data.utdanningslop?.utdanninger) {
    return null;
  }

  return data.utdanningslop;
}
