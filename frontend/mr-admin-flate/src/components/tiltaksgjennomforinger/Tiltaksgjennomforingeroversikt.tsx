import { Tiltaksgjennomforingrad } from "./Tiltaksgjennomforing";
import { useTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforinger";
import styles from "./Tiltaksgjennomforingeroversikt.module.scss";

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
      {tiltaksgjennomforinger.map((tiltaksgjennomforing) => (
        <Tiltaksgjennomforingrad
          key={tiltaksgjennomforing.id}
          tiltaksgjennomforing={tiltaksgjennomforing}
        />
      ))}
    </ul>
  );
}
