import { useUpsertAvtale } from "@/api/avtaler/useUpsertAvtale";
import {
  AvtaleFormValues,
  AvtaleFormInput,
  avtaleFormSchema,
  defaultAvtaleData,
} from "@/schemas/avtale";
import { zodResolver } from "@hookform/resolvers/zod";
import { AvtaleDto, Prismodell, ValidationError } from "@mr/api-client-v2";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { useNavigate } from "react-router";
import { useCallback } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { Separator } from "../detaljside/Metadata";
import { AvtaleFormKnapperad } from "./AvtaleFormKnapperad";
import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { getUtdanningslop } from "@/schemas/avtaledetaljer";

interface Props {
  avtale: AvtaleDto;
  children: React.ReactNode;
}

export function RedigerAvtaleContainer({ avtale, children }: Props) {
  const mutation = useUpsertAvtale();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { data: ansatt } = useHentAnsatt();

  const methods = useForm<AvtaleFormInput, any, AvtaleFormValues>({
    resolver: zodResolver(avtaleFormSchema),
    defaultValues: defaultAvtaleData(ansatt, avtale),
  });

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
      onSuccess: (dto: { data: AvtaleDto }) => {
        queryClient.setQueryData(QueryKeys.avtale(dto.data.id), dto.data);
        navigate(`/avtaler/${dto.data.id}`);
      },
      onValidationError: (error: ValidationError) => {
        handleValidationError(error);
      },
    });
  };

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
        <AvtaleFormKnapperad />
        <Separator />
        {children}
      </form>
    </FormProvider>
  );
}
