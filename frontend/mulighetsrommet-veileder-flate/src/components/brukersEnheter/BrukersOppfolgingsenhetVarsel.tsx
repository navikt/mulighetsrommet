import { Alert } from "@navikt/ds-react";
import { Bruker } from "mulighetsrommet-api-client";
import { brukersGeografiskeOgOppfolgingsenhetErLokalkontorMenIkkeSammeKontor } from "../../utils/Utils";

interface Props {
  brukerdata: Bruker;
}

export function BrukersOppfolgingsenhetVarsel({ brukerdata }: Props) {
  if (brukersGeografiskeOgOppfolgingsenhetErLokalkontorMenIkkeSammeKontor(brukerdata)) {
    return (
      <Alert style={{ marginBottom: "1rem" }} variant="info">
        Bruker har en annen oppfølgingsenhet enn geografisk enhet. Det er aktuelle tiltak knyttet
        til brukers oppfølgingsenhet som vises i listen.
      </Alert>
    );
  }
  return null;
}
