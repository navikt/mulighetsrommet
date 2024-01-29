import { Alert } from "@navikt/ds-react";
import { Bruker, VeilederflateTiltakstype } from "mulighetsrommet-api-client";
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
  const innsatsgruppeEllerTiltakstype = tiltakstype.innsatsgruppe?.tittel ?? tiltakstype.navn;

  return !brukerHarRettPaaTiltak && brukerdata.innsatsgruppe ? (
    <Alert variant="warning" className={styles.varsel}>
      Brukeren tilhører innsatsgruppen{" "}
      <strong>{brukerdata.innsatsgruppe.replaceAll("_", " ").toLocaleLowerCase()}</strong>, men
      tiltaksgjennomføringen gjelder for{" "}
      <strong>{innsatsgruppeEllerTiltakstype.toLocaleLowerCase()}</strong>.
    </Alert>
  ) : null;
}
