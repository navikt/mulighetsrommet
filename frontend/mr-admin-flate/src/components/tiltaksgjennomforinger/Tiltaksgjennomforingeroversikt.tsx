import { Tiltaksgjennomforingrad } from "./Tiltaksgjennomforing";
import { useTiltaksgjennomforing } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforing";
import styles from "./Tiltaksgjennomforingeroversikt.module.scss";

export function Tiltaksgjennomforingeroversikt() {
  const { data, isLoading } = useTiltaksgjennomforing();
  if (isLoading) {
    return null;
  }
  if (!data) {
    return null;
  }
  const { data: tiltaksgjennomforinger } = data;

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
