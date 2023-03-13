import { Nokkeltall } from "../../../components/nokkeltall/Nokkeltall";
import styles from "./NokkeltallForTiltakstype.module.scss";
import { useNokkeltallForTiltakstype } from "../../../api/tiltakstyper/useNokkeltallForTiltakstype";
import { Laster } from "../../../components/Laster";

export function NokkeltallForTiltakstype() {
  const { data } = useNokkeltallForTiltakstype();

  if (!data) {
    return <Laster tekst={"Henter nøkkeltall..."} />;
  }

  return (
    <div>
      <section className={styles.summary_container}>
        <Nokkeltall title="Avtaler" subtitle="hittil i år" value="12 598" />
        <Nokkeltall
          title="Gjennomføringer"
          subtitle="hittil i år"
          value={data.antallTiltaksgjennomforinger}
        />
      </section>
    </div>
  );
}
