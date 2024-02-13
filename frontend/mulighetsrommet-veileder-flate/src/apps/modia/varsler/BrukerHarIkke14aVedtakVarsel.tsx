import { Alert } from "@navikt/ds-react";
import { Bruker } from "mulighetsrommet-api-client";
import styles from "./BrukerKvalifisererIkkeVarsel.module.scss";

interface Props {
  brukerdata: Bruker;
}

export function BrukerHarIkke14aVedtakVarsel({ brukerdata }: Props) {
  return !brukerdata.innsatsgruppe ? (
    <Alert variant="warning" className={styles.varsel} data-testid="varsel_servicesgruppe">
      Brukeren har ikke fått §14 a-vedtak enda, og kan derfor ikke meldes på noen tiltak.
    </Alert>
  ) : (
    <></>
  );
}
