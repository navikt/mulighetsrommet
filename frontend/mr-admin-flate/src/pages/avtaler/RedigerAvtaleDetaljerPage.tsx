import { useUpsertDetaljer } from "@/api/avtaler/useUpsertDetaljer";
import { AvtaleDetaljerForm } from "@/components/avtaler/AvtaleDetaljerForm";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useEditForm } from "@/hooks/useEditForm";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { AvtaleFormValues, defaultAvtaleData } from "@/schemas/avtale";
import { avtaleDetaljerFormSchema } from "@/schemas/avtaledetaljer";
import { mapNameToSchemaPropertyName, toDetaljerRequest } from "./avtaleFormUtils";
import { RedigerAvtalePageLayout } from "./RedigerAvtalePageLayout";
import { DetaljerRequest } from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";

export function RedigerAvtaleDetaljerPage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const navigate = useNavigate();
  const { data: avtale } = useAvtale(avtaleId);
  const { data: ansatt } = useHentAnsatt();

  const { methods, onSubmit } = useEditForm<AvtaleFormValues, DetaljerRequest>({
    schema: avtaleDetaljerFormSchema,
    defaultValues: defaultAvtaleData(ansatt, avtale),
    mutation: useUpsertDetaljer(avtaleId),
    toRequest: (data) => toDetaljerRequest({ data }),
    onSuccess: () => navigate(`/avtaler/${avtaleId}`),
    mapErrorFieldName: mapNameToSchemaPropertyName,
  });

  return (
    <RedigerAvtalePageLayout seksjonsnavn="avtaledetaljer" methods={methods} onSubmit={onSubmit}>
      <AvtaleDetaljerForm />
    </RedigerAvtalePageLayout>
  );
}
