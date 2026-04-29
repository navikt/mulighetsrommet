import { useUpsertVeilederinformasjon } from "@/api/avtaler/useUpsertVeilederinformasjon";
import { AvtaleInformasjonForVeiledereForm } from "@/components/avtaler/AvtaleInformasjonForVeiledereForm";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAvtale } from "@/api/avtaler/useAvtale";
import {
  defaultAvtaleData,
  VeilederinfoInputValues,
  VeilederinfoOutputValues,
  VeilederinformasjonStepSchema,
} from "@/schemas/avtale";
import { toVeilederinfoRequest } from "./avtaleFormUtils";
import { RedigerAvtalePageLayout } from "./RedigerAvtalePageLayout";
import { ValidationError } from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";
import { FormContainer } from "@/components/skjema/FormContainer";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { applyValidationErrors } from "@/components/skjema/helpers";

export function RedigerAvtaleVeilederinformasjonPage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const navigate = useNavigate();
  const { data: avtale } = useAvtale(avtaleId);
  const { data: ansatt } = useHentAnsatt();
  const mutation = useUpsertVeilederinformasjon(avtaleId);

  const methods = useForm<VeilederinfoInputValues, unknown, VeilederinfoOutputValues>({
    resolver: zodResolver(VeilederinformasjonStepSchema),
    defaultValues: defaultAvtaleData(ansatt, avtale),
  });

  const onSubmit = methods.handleSubmit((data) => {
    mutation.mutate(toVeilederinfoRequest(data), {
      onSuccess: () => navigate(`/avtaler/${avtaleId}/veilederinformasjon`),
      onValidationError: (validation: ValidationError) => {
        applyValidationErrors(methods, validation);
      },
    });
  });

  return (
    <RedigerAvtalePageLayout>
      <FormContainer
        heading="Redigerer informasjon for veiledere"
        methods={methods}
        onSubmit={onSubmit}
      >
        <AvtaleInformasjonForVeiledereForm />
      </FormContainer>
    </RedigerAvtalePageLayout>
  );
}
