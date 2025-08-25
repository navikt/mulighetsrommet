import { Alert } from "@navikt/ds-react";
import { Brukerdata, BrukerdataVarsel } from "@api-client";
import { useArbeidsmarkedstiltakFilterValue } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { brukersEnhetFilterHasChanged } from "@/apps/modia/delMedBruker/helpers";

interface Props {
  brukerdata: Brukerdata;
}

export function BrukersOppfolgingsenhetVarsel({ brukerdata }: Props) {
  const filter = useArbeidsmarkedstiltakFilterValue();

  return !brukersEnhetFilterHasChanged(filter, brukerdata) &&
    brukerdata.varsler.includes(BrukerdataVarsel.LOKAL_OPPFOLGINGSENHET) ? (
    <Alert variant="info">
      Bruker har en annen oppfølgingsenhet enn geografisk enhet. Det er aktuelle tiltak knyttet til
      brukers oppfølgingsenhet som vises i listen.
    </Alert>
  ) : null;
}
