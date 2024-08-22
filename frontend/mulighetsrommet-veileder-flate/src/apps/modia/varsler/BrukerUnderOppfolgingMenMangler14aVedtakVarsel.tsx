import { Alert } from "@navikt/ds-react";
import { Bruker, BrukerVarsel } from "@mr/api-client";

interface Props {
  brukerdata: Bruker;
}

export function BrukerUnderOppfolgingMenMangler14aVedtakVarsel({ brukerdata }: Props) {
  return brukerdata.varsler.includes(
    BrukerVarsel.BRUKER_UNDER_OPPFOLGING_MEN_MANGLER_14A_VEDTAK,
  ) ? (
    <Alert variant="warning" data-testid="varsel_servicesgruppe">
      Brukeren har ikke fått §14 a-vedtak, og kan derfor ikke meldes på noen tiltak.
    </Alert>
  ) : null;
}
