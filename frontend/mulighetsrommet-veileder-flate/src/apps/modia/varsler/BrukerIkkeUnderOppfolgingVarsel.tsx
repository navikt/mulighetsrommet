import { Alert } from "@navikt/ds-react";
import { Brukerdata, BrukerdataVarsel } from "@api-client";

interface Props {
  brukerdata: Brukerdata;
}

export function BrukerIkkeUnderOppfolgingVarsel({ brukerdata }: Props) {
  if (brukerdata.varsler.includes(BrukerdataVarsel.BRUKER_IKKE_UNDER_OPPFOLGING)) {
    return (
      <Alert variant="warning">
        Bruker er ikke under oppfølging, og kan derfor ikke meldes på noen tiltak.
      </Alert>
    );
  }

  return null;
}
