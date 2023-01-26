import { Alert } from "@navikt/ds-react";
import { TiltaksgruppeRad } from "./TiltaksgruppeRad";
import styles from "../listeelementer/Listeelementer.module.scss";
import { useTiltaksgrupper } from "../../api/tiltaksgrupper/useTiltaksgrupper";
import { ListeheaderTiltaksgrupper } from "../listeelementer/Listeheader";

export function TiltaksgrupperOversikt() {
  const tiltaksgrupper = useTiltaksgrupper();

  return (
    <>
      {tiltaksgrupper.length === 0 ? (
        <Alert variant="info">Vi fant ingen tiltakstyper</Alert>
      ) : (
        <ul className={styles.oversikt}>
          {tiltaksgrupper.length === 0 ? (
            <Alert variant="info">Vi fant ingen tiltaksgrupper</Alert>
          ) : null}
          <ListeheaderTiltaksgrupper />
          {tiltaksgrupper.map((tiltaksgruppe) => (
            <TiltaksgruppeRad
              key={tiltaksgruppe.id}
              tiltaksgruppe={tiltaksgruppe}
            />
          ))}
        </ul>
      )}
    </>
  );
}
