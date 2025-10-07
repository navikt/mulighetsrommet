import {
  AvtaleFormInput,
  avtaleFormSchema,
  AvtaleFormValues,
  defaultAvtaleData,
} from "@/schemas/avtale";
import { zodResolver } from "@hookform/resolvers/zod";
import { AvtaleDto, ProblemDetail, ValidationError } from "@tiltaksadministrasjon/api-client";
import { useLocation, useNavigate } from "react-router";
import { ReactNode, useCallback } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useQueryClient } from "@tanstack/react-query";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { QueryKeys } from "@/api/QueryKeys";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { mapNameToSchemaPropertyName } from "@/pages/avtaler/avtaleFormUtils";
import { ApiMutationResult } from "@/hooks/useApiMutation";

interface Props<TRequest> {
  avtale: AvtaleDto;
  mapToRequest: (data: AvtaleFormValues, avtale?: AvtaleDto) => TRequest;
  mutation: ApiMutationResult<{ data: AvtaleDto }, ProblemDetail, TRequest, unknown>;
  children: ReactNode;
}

export function RedigerAvtaleContainer<TRequest>({
  avtale,
  children,
  mutation,
  mapToRequest,
}: Props<TRequest>) {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const nav = pathname.replace("/skjema", "");
  const queryClient = useQueryClient();
  const { data: ansatt } = useHentAnsatt();

  const methods = useForm<AvtaleFormInput, any, AvtaleFormValues>({
    resolver: zodResolver(avtaleFormSchema),
    defaultValues: defaultAvtaleData(ansatt, avtale),
  });

  const handleValidationError = useCallback(
    (validation: ValidationError) => {
      validation.errors.forEach((error) => {
        const name = mapNameToSchemaPropertyName(jsonPointerToFieldPath(error.pointer));
        methods.setError(name, { type: "custom", message: error.detail });
      });
    },
    [methods],
  );

  const onSubmit = async (data: AvtaleFormValues) =>
    mutation.mutate(mapToRequest(data, avtale), {
      onValidationError: (error: ValidationError) => {
        handleValidationError(error);
      },
      onSuccess: (dto: { data: AvtaleDto }) => {
        queryClient.setQueryData(QueryKeys.avtale(dto.data.id), dto.data);
        navigate(nav);
      },
    });

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onSubmit)}>{children}</form>
    </FormProvider>
  );
}
