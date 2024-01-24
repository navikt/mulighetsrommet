import { Alert } from "@navikt/ds-react";
import { Bruker } from "mulighetsrommet-api-client";
import {
  brukersEnhetFilterHasChanged,
  brukersGeografiskeOgOppfolgingsenhetErLokalkontorMenIkkeSammeKontor,
} from "../../utils/Utils";
import { useArbeidsmarkedstiltakFilterValue } from "../../hooks/useArbeidsmarkedstiltakFilter";

interface Props {
  brukerdata: Bruker;
}

export function BrukersOppfolgingsenhetVarsel({ brukerdata }: Props) {
  const filter = useArbeidsmarkedstiltakFilterValue();

  if (
    !brukersEnhetFilterHasChanged(filter, brukerdata) &&
    brukersGeografiskeOgOppfolgingsenhetErLokalkontorMenIkkeSammeKontor(brukerdata)
  ) {
    return (
      <Alert style={{ marginBottom: "1rem" }} variant="info">
        Bruker har en annen oppfølgingsenhet enn geografisk enhet. Det er aktuelle tiltak knyttet
        til brukers oppfølgingsenhet som vises i listen.
      </Alert>
    );
  }
  return null;
}
