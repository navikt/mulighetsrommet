import { Alert } from "@navikt/ds-react";
import { TiltaksgruppeRad } from "./TiltaksgruppeRad";
import styles from "./Tiltaksgrupperoversikt.module.scss";
import { useTiltaksgrupper } from "../../api/tiltaksgrupper/useTiltaksgrupper";

export function TiltaksgruppeOversikt() {
  const tiltaksgrupper = useTiltaksgrupper();

  return (
    <>
      <ul className={styles.oversikt}>
        {tiltaksgrupper.length === 0 ? (
          <Alert variant="info">Vi fant ingen tiltaksgrupper</Alert>
        ) : null}
        {tiltaksgrupper.map((tiltaksgruppe) => (
          <TiltaksgruppeRad
            key={tiltaksgruppe.id}
            tiltaksgruppe={tiltaksgruppe}
          />
        ))}
      </ul>
    </>
  );
}
