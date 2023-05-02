import { Alert, BodyShort } from "@navikt/ds-react";
import { ContainerLayout } from "../../layouts/ContainerLayout";

export function NotifikasjonerPage() {
  return (
    <ContainerLayout>
      <Alert variant="info">
        <BodyShort>
          Her kommer det funksjonalitet for å vise varsler og beskjeder.
        </BodyShort>
      </Alert>
    </ContainerLayout>
  );
}
