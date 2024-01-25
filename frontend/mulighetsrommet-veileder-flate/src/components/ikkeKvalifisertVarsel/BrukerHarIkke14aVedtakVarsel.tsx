import { Alert } from "@navikt/ds-react";
import { Bruker, BrukerVarsel } from "mulighetsrommet-api-client";
import styles from "./BrukerKvalifisererIkkeVarsel.module.scss";

interface Props {
  brukerdata: Bruker;
}

export function BrukerHarIkke14aVedtakVarsel({ brukerdata }: Props) {
  return brukerdata.varsler.includes(BrukerVarsel.MANGLER_14A_VEDTAK) ? (
    <Alert variant="warning" className={styles.varsel} data-testid="varsel_servicesgruppe">
      Brukeren har ikke fått §14 a-vedtak enda, og kan derfor ikke meldes på noen tiltak.
    </Alert>
  ) : (
    <></>
  );
}
