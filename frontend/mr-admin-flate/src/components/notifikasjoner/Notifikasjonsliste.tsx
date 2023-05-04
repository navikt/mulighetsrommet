import { useFeatureToggles } from "../../api/features/feature-toggles";
import styles from "./BrukerNotifikasjoner.module.scss";
import { useNotifikasjonerForAnsatt } from "../../api/notifikasjoner/useNotifikasjonerForAnsatt";
import { Notifikasjonssrad } from "./Notifikasjonssrad";
import { Laster } from "../laster/Laster";
import { Alert, BodyShort } from "@navikt/ds-react";

export function Notifikasjonsliste() {
  const { data: features } = useFeatureToggles();
  const { isLoading, data: paginertResultat } = useNotifikasjonerForAnsatt();

  if (!features?.["mulighetsrommet.admin-flate-se-notifikasjoner"]) return null;

  if (isLoading && !paginertResultat) {
    return <Laster />;
  }

  if (!paginertResultat) {
    return null;
  }

  const { data } = paginertResultat;

  if (data.length !== 0) {
    return (
      <Alert variant="info">
        <BodyShort>Du har ingen nye varsler.</BodyShort>
      </Alert>
    );
  }

  return (
    <section className={styles.container}>
      {data.map((n) => {
        return (
          <Notifikasjonssrad key={n.id} notifikasjon={n}></Notifikasjonssrad>
        );
      })}
    </section>
  );
}
