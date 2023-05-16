import { useFeatureToggles } from "../../api/features/feature-toggles";
import styles from "./BrukerNotifikasjoner.module.scss";
import { useNotifikasjonerForAnsatt } from "../../api/notifikasjoner/useNotifikasjonerForAnsatt";
import { UlestNotifikasjonssrad } from "./UlestNotifikasjonssrad";
import { Laster } from "../laster/Laster";
import { Notifikasjonsstatus } from "mulighetsrommet-api-client";
import { EmptyState } from "./EmptyState";

export function UlesteNotifikasjonsliste() {
  const { data: features } = useFeatureToggles();
  const { isLoading, data: paginertResultat } = useNotifikasjonerForAnsatt(
    Notifikasjonsstatus.UNREAD
  );

  if (!features?.["mulighetsrommet.admin-flate-se-notifikasjoner"]) return null;

  if (isLoading && !paginertResultat) {
    return <Laster />;
  }

  if (!paginertResultat) {
    return null;
  }

  const { data = [] } = paginertResultat;

  if (data.length === 0) {
    return (
      <EmptyState
        tittel={"Ingen nye varsler"}
        beskrivelse={"Vi varsler deg når noe skjer"}
      />
    );
  }

  return (
    <ul className={styles.container}>
      {data.map((n) => {
        return (
          <UlestNotifikasjonssrad
            key={n.id}
            notifikasjon={n}
          ></UlestNotifikasjonssrad>
        );
      })}
    </ul>
  );
}
