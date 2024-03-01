import { Alert } from "@navikt/ds-react";
import { Bruker } from "mulighetsrommet-api-client";

interface Props {
  brukerdata: Bruker;
}

export function BrukerHarIkke14aVedtakVarsel({ brukerdata }: Props) {
  return !brukerdata.innsatsgruppe ? (
    <Alert variant="warning" data-testid="varsel_servicesgruppe">
      Brukeren har ikke fått §14 a-vedtak enda, og kan derfor ikke meldes på noen tiltak.
    </Alert>
  ) : null;
}
