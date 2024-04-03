import { Alert } from "@navikt/ds-react";
import { Bruker, BrukerVarsel } from "mulighetsrommet-api-client";

interface Props {
  brukerdata: Bruker;
}

export function BrukerErIkkeUnderOppfolgingVarsel({ brukerdata }: Props) {
  if (brukerdata.varsler.includes(BrukerVarsel.BRUKER_IKKE_UNDER_OPPFOLGING)) {
    return <Alert variant="warning">Bruker er ikke under oppfølging</Alert>;
  }

  return null;
}
