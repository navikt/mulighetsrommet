import { Alert } from "@navikt/ds-react";
import { Bruker, VeilederflateTiltakstype } from "mulighetsrommet-api-client";
import appStyles from "../../App.module.scss";
import styles from "./BrukerKvalifisererIkkeVarsel.module.scss";

interface Props {
  brukerdata: Bruker;
  brukerHarRettPaaTiltak: boolean;
  tiltakstype: VeilederflateTiltakstype;
}

export function BrukerKvalifisererIkkeVarsel({
  brukerHarRettPaaTiltak,
  brukerdata,
  tiltakstype,
}: Props) {
  const innsatsgruppeEllerTiltakstype =
    tiltakstype.innsatsgruppe?.nokkel?.replaceAll("_", " ") ?? tiltakstype.navn;

  return !brukerHarRettPaaTiltak && brukerdata.innsatsgruppe ? (
    <Alert variant="warning" className={styles.varsel}>
      Brukeren tilhører innsatsgruppen{" "}
      <strong className={appStyles.lowercase}>
        {brukerdata.innsatsgruppe.replaceAll("_", " ")}
      </strong>
      , men tiltaksgjennomføringen gjelder for{" "}
      <strong className={appStyles.lowercase}>{innsatsgruppeEllerTiltakstype}</strong>.
    </Alert>
  ) : null;
}
