import { Alert } from "@navikt/ds-react";
import { Brukerdata, BrukerVarsel } from "@api-client";

interface Props {
  brukerdata: Brukerdata;
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
