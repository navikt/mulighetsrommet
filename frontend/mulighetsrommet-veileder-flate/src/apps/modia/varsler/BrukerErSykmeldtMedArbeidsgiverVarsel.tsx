import { Alert } from "@navikt/ds-react";
import { Bruker, Innsatsgruppe } from "@mr/api-client";

interface Props {
  brukerdata: Bruker;
}

export function BrukerErSykmeldtMedArbeidsgiverVarsel({ brukerdata }: Props) {
  return brukerdata.erSykmeldtMedArbeidsgiver &&
    brukerdata.innsatsgruppe == Innsatsgruppe.STANDARD_INNSATS ? (
    <Alert variant="info">
      Bruker er sykmeldt med arbedsgiver og kan derfor også meldes på Arbeidsrettet rehabilitering.
    </Alert>
  ) : null;
}
