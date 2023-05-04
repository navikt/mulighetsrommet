import { Alert, BodyShort, Heading } from "@navikt/ds-react";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { Notifikasjonsliste } from "../../components/notifikasjoner/Notifikasjonsliste";

export function NotifikasjonerPage() {
  return (
    <ContainerLayout>
      <Heading size={"medium"}>Varsler</Heading>
      <Notifikasjonsliste />
    </ContainerLayout>
  );
}
