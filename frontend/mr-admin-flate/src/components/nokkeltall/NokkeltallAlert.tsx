import { Alert } from "@navikt/ds-react";
import styles from "./Nokkeltall.module.scss";

export const NokkeltallAlert = () => {
  return (
    <Alert variant="warning" className={styles.alert}>
      Tjenesten er under utvikling og tallene som vises her under nøkkeltall kan
      være feil eller misvisende pga. feil eller for dårlig datagrunnlag.
    </Alert>
  );
};
