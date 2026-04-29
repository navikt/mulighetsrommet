import { useUpsertPersonvern } from "@/api/avtaler/useUpsertPersonvern";
import { AvtalePersonvernForm } from "@/components/avtaler/AvtalePersonvernForm";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { defaultAvtaleData } from "@/pages/avtaler/form/defaults";
import {
  PersonopplysningerInputValues,
  PersonopplysningerOutputValues,
  PersonopplysningerSchema,
} from "@/pages/avtaler/form/validation";
import { toPersonvernRequest } from "./form/mappers";
import { RedigerAvtalePageLayout } from "./RedigerAvtalePageLayout";
import { ValidationError } from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";
import { FormContainer } from "@/components/skjema/FormContainer";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { applyValidationErrors } from "@/components/skjema/helpers";

export function RedigerAvtalePersonvernPage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const navigate = useNavigate();
  const { data: avtale } = useAvtale(avtaleId);
  const { data: ansatt } = useHentAnsatt();
  const mutation = useUpsertPersonvern(avtaleId);

  const methods = useForm<PersonopplysningerInputValues, unknown, PersonopplysningerOutputValues>({
    resolver: zodResolver(PersonopplysningerSchema),
    defaultValues: defaultAvtaleData(ansatt, avtale),
  });

  const onSubmit = methods.handleSubmit((data) => {
    mutation.mutate(toPersonvernRequest({ data }), {
      onSuccess: () => navigate(`/avtaler/${avtaleId}/personvern`),
      onValidationError: (validation: ValidationError) => {
        applyValidationErrors(methods, validation);
      },
    });
  });

  return (
    <RedigerAvtalePageLayout>
      <FormContainer heading="Redigerer personvern" methods={methods} onSubmit={onSubmit}>
        <AvtalePersonvernForm />
      </FormContainer>
    </RedigerAvtalePageLayout>
  );
}
