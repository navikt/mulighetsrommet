import { Alert } from "@navikt/ds-react";
import { Bruker, BrukerVarsel } from "mulighetsrommet-api-client";
import styles from "./BrukerKvalifisererIkkeVarsel.module.scss";

interface Props {
  brukerdata: Bruker;
}

export function ManglerInnsatsOgServicegruppeVarsel({ brukerdata }: Props) {
  return brukerdata.varsler.includes(BrukerVarsel.MANGLER_INNSATSGRUPPE_OG_SERVICEGRUPPE) ? (
    <Alert variant="error" className={styles.varsel} data-testid="varsel_innsats_og_servicesgruppe">
      Innsatsgruppe og servicegruppe mangler. Kontroller om brukeren er under oppfølging og finnes i
      Arena
    </Alert>
  ) : null;
}
