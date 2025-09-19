import { Alert } from "@navikt/ds-react";
import { Brukerdata, Innsatsgruppe } from "@api-client";

interface Props {
  brukerdata: Brukerdata;
}

export function BrukerErSykmeldtMedArbeidsgiverVarsel({ brukerdata }: Props) {
  return brukerdata.erSykmeldtMedArbeidsgiver &&
    brukerdata.innsatsgruppe === Innsatsgruppe.GODE_MULIGHETER ? (
    <Alert variant="info">
      Bruker er sykmeldt med arbedsgiver og kan derfor også meldes på Arbeidsrettet rehabilitering.
    </Alert>
  ) : null;
}
