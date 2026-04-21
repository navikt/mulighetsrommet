import { useUpsertPersonvern } from "@/api/avtaler/useUpsertPersonvern";
import { AvtalePersonvernForm } from "@/components/avtaler/AvtalePersonvernForm";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useEditForm } from "@/hooks/useEditForm";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { AvtaleFormValues, defaultAvtaleData, PersonopplysningerSchema } from "@/schemas/avtale";
import { toPersonvernRequest } from "./avtaleFormUtils";
import { RedigerAvtalePageLayout } from "./RedigerAvtalePageLayout";
import { PersonvernRequest } from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";

export function RedigerAvtalePersonvernPage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const navigate = useNavigate();
  const { data: avtale } = useAvtale(avtaleId);
  const { data: ansatt } = useHentAnsatt();

  const { methods, onSubmit } = useEditForm<AvtaleFormValues, PersonvernRequest>({
    schema: PersonopplysningerSchema,
    defaultValues: defaultAvtaleData(ansatt, avtale),
    mutation: useUpsertPersonvern(avtaleId),
    toRequest: (data) => toPersonvernRequest({ data }),
    onSuccess: () => navigate(`/avtaler/${avtaleId}/personvern`),
  });

  return (
    <RedigerAvtalePageLayout seksjonsnavn="personvern" methods={methods} onSubmit={onSubmit}>
      <AvtalePersonvernForm />
    </RedigerAvtalePageLayout>
  );
}
