import { useUpsertAvtale } from "@/api/avtaler/useUpsertAvtale";
import {
  AvtaleFormValues,
  AvtaleFormInput,
  avtaleFormSchema,
  defaultAvtaleData,
} from "@/schemas/avtale";
import { zodResolver } from "@hookform/resolvers/zod";
import { AvtaleDto, ValidationError } from "@mr/api-client-v2";
import { useNavigate } from "react-router";
import { useCallback } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { Separator } from "../detaljside/Metadata";
import { AvtaleFormKnapperad } from "./AvtaleFormKnapperad";
import { useQueryClient } from "@tanstack/react-query";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { QueryKeys } from "@/api/QueryKeys";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { mapNameToSchemaPropertyName, onSubmitAvtaleForm } from "@/pages/avtaler/avtaleFormUtils";

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
    onSubmitAvtaleForm({
      avtale,
      data,
      mutation,
      onValidationError: (error: ValidationError) => {
        handleValidationError(error);
      },
      onSuccess: (dto: { data: AvtaleDto }) => {
        queryClient.setQueryData(QueryKeys.avtale(dto.data.id), dto.data);
        navigate(`/avtaler/${dto.data.id}`);
      },
    });

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onSubmit)}>
        <ContentBox>
          <WhitePaddedBox>
            <AvtaleFormKnapperad />
            <Separator />
            {children}
          </WhitePaddedBox>
        </ContentBox>
      </form>
    </FormProvider>
  );
}
