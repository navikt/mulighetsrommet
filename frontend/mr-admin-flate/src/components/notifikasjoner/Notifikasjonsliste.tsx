import { NotificationStatus } from "mulighetsrommet-api-client";
import { useNotifikasjonerForAnsatt } from "@/api/notifikasjoner/useNotifikasjonerForAnsatt";
import { Laster } from "../laster/Laster";
import { EmptyState } from "./EmptyState";
import styles from "./Notifikasjoner.module.scss";
import { Notifikasjonssrad } from "./Notifikasjonsrad";
import { ReloadAppErrorBoundary } from "mulighetsrommet-frontend-common";

interface Props {
  lest: boolean;
}

export function Notifikasjonsliste({ lest }: Props) {
  const { isLoading, data: paginertResultat } = useNotifikasjonerForAnsatt(
    lest ? NotificationStatus.DONE : NotificationStatus.NOT_DONE,
  );

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
        tittel={lest ? "Du har ingen tidligere notifikasjoner" : "Ingen nye notifikasjoner"}
        beskrivelse={
          lest
            ? "Når du har gjort en oppgave eller lest en beskjed havner de her"
            : "Vi varsler deg når noe skjer"
        }
      />
    );
  }

  return (
    <ReloadAppErrorBoundary>
      <ul className={styles.notifikasjonsliste_ul}>
        {data.map((n) => {
          return <Notifikasjonssrad lest={lest} key={n.id} notifikasjon={n} />;
        })}
      </ul>
    </ReloadAppErrorBoundary>
  );
}
