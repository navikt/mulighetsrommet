import { Nokkeltall } from "../../../components/nokkeltall/Nokkeltall";
import styles from "./NokkeltallForTiltakstype.module.scss";
import { useNokkeltallForTiltakstype } from "../../../api/tiltakstyper/useNokkeltallForTiltakstype";
import { Laster } from "../../../components/Laster";
import { formaterTall } from "../../../utils/Utils";
import { Alert } from "@navikt/ds-react";

export function NokkeltallForTiltakstype() {
  const { data, isLoading } = useNokkeltallForTiltakstype();

  if (isLoading) {
    return <Laster tekst={"Henter nøkkeltall..."} />;
  }

  if (!data) {
    return <Alert variant={"error"}>Fant ingen nøkkeltall</Alert>;
  }

  return (
    <div>
      <section className={styles.summary_container}>
        <Nokkeltall
          title="Avtaler"
          subtitle="hittil i år"
          value={formaterTall(data.antallAvtaler)}
        />
        <Nokkeltall
          title="Gjennomføringer"
          subtitle="hittil i år"
          value={formaterTall(data.antallTiltaksgjennomforinger)}
        />
      </section>
    </div>
  );
}
