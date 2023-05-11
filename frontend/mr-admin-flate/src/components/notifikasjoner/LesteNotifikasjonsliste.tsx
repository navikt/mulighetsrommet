import { Notifikasjonsstatus } from "mulighetsrommet-api-client";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { useNotifikasjonerForAnsatt } from "../../api/notifikasjoner/useNotifikasjonerForAnsatt";
import { Laster } from "../laster/Laster";
import styles from "./BrukerNotifikasjoner.module.scss";
import { EmptyState } from "./EmptyState";
import { LestNotifikasjonssrad } from "./LestNotifikasjonssrad";

export function LesteNotifikasjonsliste() {
  const { data: features } = useFeatureToggles();
  const { isLoading, data: paginertResultat } = useNotifikasjonerForAnsatt(
    Notifikasjonsstatus.READ
  );

  if (!features?.["mulighetsrommet.admin-flate-se-notifikasjoner"]) return null;

  if (isLoading && !paginertResultat) {
    return <Laster />;
  }

  if (!paginertResultat) {
    return null;
  }

  const { data } = paginertResultat;

  if (data.length === 0) {
    return (
      <EmptyState
        tittel={"Du har ingen tidligere varsler"}
        beskrivelse={
          "Når du har gjort en oppgave eller lest en beskjed havner de her"
        }
      />
    );
  }

  return (
    <ul className={styles.container}>
      {data.map((n) => {
        return (
          <LestNotifikasjonssrad
            key={n.id}
            notifikasjon={n}
          ></LestNotifikasjonssrad>
        );
      })}
    </ul>
  );
}
