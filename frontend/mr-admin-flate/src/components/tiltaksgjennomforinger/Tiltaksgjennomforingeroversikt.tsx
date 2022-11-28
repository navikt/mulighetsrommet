import { Tiltaksgjennomforingrad } from "./Tiltaksgjennomforing";
import { useTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforinger";
import styles from "./Tiltaksgjennomforingeroversikt.module.scss";
import { Alert } from "@navikt/ds-react";

export function Tiltaksgjennomforingeroversikt() {
  const { data, isLoading } = useTiltaksgjennomforinger();
  if (isLoading) {
    return null;
  }
  if (!data) {
    return null;
  }
  const { data: tiltaksgjennomforinger, pagination: paginering } = data;

  return (
    <ul className={styles.oversikt}>
      {tiltaksgjennomforinger.length === 0 ? (
        <Alert variant="info">Vi fant ingen tiltaksgjennomføringer</Alert>
      ) : null}
      {tiltaksgjennomforinger.map((tiltaksgjennomforing) => (
        <Tiltaksgjennomforingrad
          key={tiltaksgjennomforing.id}
          tiltaksgjennomforing={tiltaksgjennomforing}
        />
      ))}
    </ul>
  );
}
