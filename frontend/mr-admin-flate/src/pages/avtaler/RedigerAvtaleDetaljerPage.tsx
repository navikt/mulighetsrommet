import { useUpsertDetaljer } from "@/api/avtaler/useUpsertDetaljer";
import { AvtaleDetaljerForm } from "@/components/avtaler/AvtaleDetaljerForm";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { defaultAvtaleData } from "@/schemas/avtale";
import {
  avtaleDetaljerFormSchema,
  AvtaleDetaljerInputValues,
  AvtaleDetaljerOutputValues,
} from "@/schemas/avtaledetaljer";
import { toDetaljerRequest } from "./avtaleFormUtils";
import { RedigerAvtalePageLayout } from "./RedigerAvtalePageLayout";
import { ValidationError } from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";
import { FormContainer } from "@/components/skjema/FormContainer";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { applyValidationErrors } from "@/components/skjema/helpers";

export function RedigerAvtaleDetaljerPage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const navigate = useNavigate();
  const { data: avtale } = useAvtale(avtaleId);
  const { data: ansatt } = useHentAnsatt();
  const mutation = useUpsertDetaljer(avtaleId);

  const methods = useForm<AvtaleDetaljerInputValues, unknown, AvtaleDetaljerOutputValues>({
    resolver: zodResolver(avtaleDetaljerFormSchema),
    defaultValues: defaultAvtaleData(ansatt, avtale),
  });

  const onSubmit = methods.handleSubmit((data) => {
    mutation.mutate(toDetaljerRequest({ data }), {
      onSuccess: () => navigate(`/avtaler/${avtaleId}`),
      onValidationError: (validation: ValidationError) => {
        applyValidationErrors(methods, validation);
      },
    });
  });

  return (
    <RedigerAvtalePageLayout>
      <FormContainer heading="Redigerer detaljer" methods={methods} onSubmit={onSubmit}>
        <AvtaleDetaljerForm />
      </FormContainer>
    </RedigerAvtalePageLayout>
  );
}
