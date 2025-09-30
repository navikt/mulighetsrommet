import {
  AvtaleFormInput,
  avtaleSchema,
  AvtaleFormValues,
  defaultAvtaleData,
} from "@/schemas/avtale";
import { zodResolver } from "@hookform/resolvers/zod";
import { ValidationError as LegacyValidationError } from "@mr/api-client-v2";
import { AvtaleDto, ProblemDetail, ValidationError } from "@tiltaksadministrasjon/api-client";
import { ReactNode, useCallback } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { QueryKeys } from "@/api/QueryKeys";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { mapNameToSchemaPropertyName } from "@/pages/avtaler/avtaleFormUtils";
import { ApiMutationResult } from "@/hooks/useApiMutation";
import { useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router";

interface Props<TRequest> {
  avtale: AvtaleDto;
  mapToRequest: (data: AvtaleFormValues) => TRequest;
  mutation: ApiMutationResult<{ data: AvtaleDto }, ProblemDetail, TRequest, unknown>;
  children: ReactNode;
}

export function RedigerAvtaleContainer<TRequest>({
  avtale,
  mutation,
  mapToRequest,
  children,
}: Props<TRequest>) {
  const { data: ansatt } = useHentAnsatt();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

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
    mutation.mutate(mapToRequest(data), {
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
