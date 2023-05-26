import { useFeatureToggles } from "../../api/features/feature-toggles";
import { useNotifikasjonerForAnsatt } from "../../api/notifikasjoner/useNotifikasjonerForAnsatt";
import { Laster } from "../laster/Laster";
import { Notifikasjonsstatus } from "mulighetsrommet-api-client";
import { EmptyState } from "./EmptyState";
import styles from "./Notifikasjoner.module.scss";
import { Notifikasjonssrad } from "./Notifikasjonsrad";

interface Props {
  lest: boolean;
}

export function Notifikasjonsliste({ lest }: Props) {
  const { data: features } = useFeatureToggles();
  const { isLoading, data: paginertResultat } = useNotifikasjonerForAnsatt(
    lest ? Notifikasjonsstatus.READ : Notifikasjonsstatus.UNREAD
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
    <ul className={styles.notifikasjonsliste_ul}>
      {data.map((n) => {
        return <Notifikasjonssrad lest={lest} key={n.id} notifikasjon={n} />;
      })}
    </ul>
  );
}
