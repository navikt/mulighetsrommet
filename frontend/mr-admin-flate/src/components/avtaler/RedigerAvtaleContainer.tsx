import { useUpsertAvtale } from "@/api/avtaler/useUpsertAvtale";
import {
  AvtaleFormInput,
  avtaleSchema,
  AvtaleFormValues,
  defaultAvtaleData,
} from "@/schemas/avtale";
import { zodResolver } from "@hookform/resolvers/zod";
import { ValidationError as LegacyValidationError } from "@mr/api-client-v2";
import { AvtaleDto, ValidationError } from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";
import { ReactNode, useCallback } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useQueryClient } from "@tanstack/react-query";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { QueryKeys } from "@/api/QueryKeys";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { mapNameToSchemaPropertyName, onSubmitAvtaleForm } from "@/pages/avtaler/avtaleFormUtils";

interface Props {
  avtale: AvtaleDto;
  children: ReactNode;
}

export function RedigerAvtaleContainer({ avtale, children }: Props) {
  const mutation = useUpsertAvtale();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { data: ansatt } = useHentAnsatt();

  const methods = useForm<AvtaleFormInput, any, AvtaleFormValues>({
    resolver: zodResolver(avtaleSchema),
    defaultValues: defaultAvtaleData(ansatt, avtale),
  });

  const handleValidationError = useCallback(
    (validation: ValidationError | LegacyValidationError) => {
      validation.errors.forEach((error) => {
        const name = mapNameToSchemaPropertyName(jsonPointerToFieldPath(error.pointer));
        methods.setError(name, { type: "custom", message: error.detail });
      });
    },
    [methods],
  );

  const onSubmit = async (data: AvtaleFormValues) =>
    onSubmitAvtaleForm({
      avtale,
      data,
      mutation,
      onValidationError: (error: ValidationError | LegacyValidationError) => {
        handleValidationError(error);
      },
      onSuccess: (dto: { data: AvtaleDto }) => {
        queryClient.setQueryData(QueryKeys.avtale(dto.data.id), dto.data);
        navigate(`/avtaler/${dto.data.id}`);
      },
    });

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onSubmit)}>{children}</form>
    </FormProvider>
  );
}
