import { useUpsertVeilederinformasjon } from "@/api/avtaler/useUpsertVeilederinformasjon";
import { AvtaleInformasjonForVeiledereForm } from "@/components/avtaler/AvtaleInformasjonForVeiledereForm";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useEditForm } from "@/hooks/useEditForm";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAvtale } from "@/api/avtaler/useAvtale";
import {
  AvtaleFormValues,
  defaultAvtaleData,
  VeilederinformasjonStepSchema,
} from "@/schemas/avtale";
import { toVeilederinfoRequest } from "./avtaleFormUtils";
import { RedigerAvtalePageLayout } from "./RedigerAvtalePageLayout";
import { VeilederinfoRequest } from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";

export function RedigerAvtaleVeilederinformasjonPage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const navigate = useNavigate();
  const { data: avtale } = useAvtale(avtaleId);
  const { data: ansatt } = useHentAnsatt();

  const { methods, onSubmit } = useEditForm<AvtaleFormValues, VeilederinfoRequest>({
    schema: VeilederinformasjonStepSchema,
    defaultValues: defaultAvtaleData(ansatt, avtale),
    mutation: useUpsertVeilederinformasjon(avtaleId),
    toRequest: (data) => toVeilederinfoRequest({ data }),
    onSuccess: () => navigate(`/avtaler/${avtaleId}/veilederinformasjon`),
  });

  return (
    <RedigerAvtalePageLayout
      seksjonsnavn="veilederinformasjon"
      methods={methods}
      onSubmit={onSubmit}
    >
      <AvtaleInformasjonForVeiledereForm />
    </RedigerAvtalePageLayout>
  );
}
