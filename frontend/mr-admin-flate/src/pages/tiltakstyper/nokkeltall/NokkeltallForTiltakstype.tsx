import { Nokkeltall } from "../../../components/nokkeltall/Nokkeltall";
import styles from "./NokkeltallForTiltakstype.module.scss";

export function NokkeltallForTiltakstype() {
  return (
    <div>
      <section className={styles.summary_container}>
        <Nokkeltall title="Avtaler" subtitle="hittil i år" value="12 598" />
        <Nokkeltall
          title="Gjennomføringer"
          subtitle="hittil i år"
          value="987"
        />
      </section>
    </div>
  );
}
