import { BellIcon } from "@navikt/aksel-icons";
import { Link } from "react-router-dom";
import { useNotificationSummary } from "@/api/notifikasjoner/useNotificationSummary";
import styles from "./Notifikasjonsbjelle.module.scss";

function Notifier() {
  return <span className={styles.notifier}></span>;
}

export function Notifikasjonsbjelle() {
  const { data: summary, isLoading: isLoadingUlesteNotifikasjoner } = useNotificationSummary();

  if (isLoadingUlesteNotifikasjoner || !summary) {
    return null;
  }

  const harUlesteNotifikasjoner = summary.notDoneCount > 0;

  return (
    <Link to="/arbeidsbenk/notifikasjoner" className={styles.lenke}>
      <div className={styles.bell_container}>
        {harUlesteNotifikasjoner ? <Notifier /> : null}
        <BellIcon fontSize={24} title="Notifikasjonsbjelle" />
      </div>
    </Link>
  );
}
