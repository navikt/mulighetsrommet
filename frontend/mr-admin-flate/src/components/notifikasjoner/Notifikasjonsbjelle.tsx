import { BellIcon } from "@navikt/aksel-icons";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { Link } from "react-router-dom";
import styles from "./Notifikasjonsbjelle.module.scss";
import { useNotificationSummary } from "../../api/notifikasjoner/useNotificationSummary";

function Notifier() {
  return <span className={styles.notifier}></span>;
}

export function Notifikasjonsbjelle() {
  const { data: features } = useFeatureToggles();
  const { data: summary, isLoading: isLoadingUlesteNotifikasjoner } =
    useNotificationSummary();

  if (isLoadingUlesteNotifikasjoner || !summary) {
    return null;
  }

  const harUlesteNotifikasjoner = summary.notDoneCount > 0;

  return features?.["mulighetsrommet.admin-flate-se-notifikasjoner"] ? (
    <Link
      to="/notifikasjoner"
      className={styles.lenke}
      data-testid="notifikasjonsbjelle"
    >
      <div className={styles.bell_container}>
        {harUlesteNotifikasjoner ? <Notifier /> : null}
        <BellIcon fontSize={24} title="Notifikasjonsbjelle" />
      </div>
    </Link>
  ) : null;
}
