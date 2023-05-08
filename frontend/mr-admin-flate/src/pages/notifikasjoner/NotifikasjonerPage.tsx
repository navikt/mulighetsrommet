import { Heading } from "@navikt/ds-react";
import { Notifikasjonsliste } from "../../components/notifikasjoner/Notifikasjonsliste";
import { ContainerLayout } from "../../layouts/ContainerLayout";

export function NotifikasjonerPage() {
  return (
    <main>
      <ContainerLayout>
        <Heading size={"medium"}>Varsler</Heading>
        <Notifikasjonsliste />
      </ContainerLayout>
    </main>
  );
}
