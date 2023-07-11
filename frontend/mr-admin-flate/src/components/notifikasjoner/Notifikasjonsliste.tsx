import { useFeatureToggles } from "../../api/features/feature-toggles";
import { useNotifikasjonerForAnsatt } from "../../api/notifikasjoner/useNotifikasjonerForAnsatt";
import { Laster } from "../laster/Laster";
import { NotificationStatus } from "mulighetsrommet-api-client";
import { EmptyState } from "./EmptyState";
import styles from "./Notifikasjoner.module.scss";
import { Notifikasjonssrad } from "./Notifikasjonsrad";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "../../main";

interface Props {
  lest: boolean;
}

export function Notifikasjonsliste({ lest }: Props) {
  const { data: features } = useFeatureToggles();
  const { isLoading, data: paginertResultat } = useNotifikasjonerForAnsatt(
    lest ? NotificationStatus.DONE : NotificationStatus.NOT_DONE
  );

  if (isLoading && !paginertResultat) {
    return <Laster />;
  }

  if (
    !paginertResultat ||
    !features?.["mulighetsrommet.admin-flate-se-notifikasjoner"]
  ) {
    return null;
  }

  const { data = [] } = paginertResultat;

  if (data.length === 0) {
    return (
      <EmptyState
        tittel={
          lest
            ? "Du har ingen tidligere notifikasjoner"
            : "Ingen nye notifikasjoner"
        }
        beskrivelse={
          lest
            ? "Når du har gjort en oppgave eller lest en beskjed havner de her"
            : "Vi varsler deg når noe skjer"
        }
      />
    );
  }

  return (
    <ErrorBoundary FallbackComponent={ErrorFallback}>
    <ul className={styles.notifikasjonsliste_ul}>
      {data.map((n) => {
        return <Notifikasjonssrad lest={lest} key={n.id} notifikasjon={n} />;
      })}
    </ul>
    </ErrorBoundary>
  );
}
