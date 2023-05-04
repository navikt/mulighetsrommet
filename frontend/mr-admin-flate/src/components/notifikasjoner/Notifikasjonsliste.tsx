import { useFeatureToggles } from "../../api/features/feature-toggles";
import styles from "./BrukerNotifikasjoner.module.scss";
import { useNotifikasjonerForAnsatt } from "../../api/notifikasjoner/useNotifikasjonerForAnsatt";
import { Notifikasjonssrad } from "./Notifikasjonssrad";
import { Laster } from "../laster/Laster";

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
