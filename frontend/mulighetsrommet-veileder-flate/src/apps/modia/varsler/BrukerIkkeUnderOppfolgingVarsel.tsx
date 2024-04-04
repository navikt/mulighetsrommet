import { Alert } from "@navikt/ds-react";
import { Bruker, BrukerVarsel } from "mulighetsrommet-api-client";

interface Props {
  brukerdata: Bruker;
}

export function BrukerIkkeUnderOppfolgingVarsel({ brukerdata }: Props) {
  return brukerdata.varsler.includes(BrukerVarsel.BRUKER_IKKE_UNDER_OPPFOLGING) ? (
    <Alert variant="warning" data-testid="varsel_servicesgruppe">
      Brukeren har ikke fått §14 a-vedtak, og kan derfor ikke meldes på noen tiltak.
    </Alert>
  ) : null;
}
