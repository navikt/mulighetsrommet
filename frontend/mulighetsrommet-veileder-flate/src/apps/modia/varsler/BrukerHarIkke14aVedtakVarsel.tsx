import { Alert } from "@navikt/ds-react";
import { Bruker } from "mulighetsrommet-api-client";
import styles from "./BrukerKvalifisererIkkeVarsel.module.scss";

interface Props {
  brukerdata: Bruker;
}

export function BrukerHarIkke14aVedtakVarsel({ brukerdata }: Props) {
  if (brukerdata.innsatsgruppe) {
    return <></>;
  }
  if (brukerdata.erSykmeldtMedArbeidsgiver) {
    return (
      <Alert variant="warning" className={styles.varsel} data-testid="varsel_servicesgruppe">
        Brukeren har ikke §14 a-vedtak, men er sykmeldt med arbeidsgiver og kan vurderes for
        tiltakene avklaring, oppfølging og arbeidsrettet rehabilitering.
      </Alert>
    );
  }
  return (
    <Alert variant="warning" className={styles.varsel} data-testid="varsel_servicesgruppe">
      Brukeren har ikke fått §14 a-vedtak enda, og kan derfor ikke meldes på noen tiltak.
    </Alert>
  );
}
