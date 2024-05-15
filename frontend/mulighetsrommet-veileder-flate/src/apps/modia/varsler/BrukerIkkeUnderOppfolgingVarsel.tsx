import { Alert } from "@navikt/ds-react";
import { Bruker, BrukerVarsel } from "mulighetsrommet-api-client";

interface Props {
  brukerdata: Bruker;
}

export function BrukerIkkeUnderOppfolgingVarsel({ brukerdata }: Props) {
  if (brukerdata.varsler.includes(BrukerVarsel.BRUKER_IKKE_UNDER_OPPFOLGING)) {
    return (
      <Alert variant="warning">
        Bruker er ikke under oppfølging, og kan derfor ikke meldes på noen tiltak.
      </Alert>
    );
  }

  return null;
}
