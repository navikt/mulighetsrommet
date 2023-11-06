import { Alert } from "@navikt/ds-react";
import { Bruker, NavEnhetType } from "mulighetsrommet-api-client";

interface Props {
  brukerdata: Bruker;
}

export function BrukersOppfolgingsenhetVarsel({ brukerdata }: Props) {
  if (
    brukerdata.oppfolgingsenhet?.type === NavEnhetType.LOKAL &&
    brukerdata?.oppfolgingsenhet.enhetsnummer !== brukerdata?.geografiskEnhet?.enhetsnummer
  ) {
    return (
      <Alert variant="info">
        Brukers oppfølgingsenhet er et lokalkontor som ikke er likt brukers geografiske enhet.
      </Alert>
    );
  }
  return null;
}
