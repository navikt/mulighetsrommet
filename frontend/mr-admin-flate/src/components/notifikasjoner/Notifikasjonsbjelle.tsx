import { BellIcon } from "@navikt/aksel-icons";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { Link } from "react-router-dom";
import styles from "./Notifikasjonsbjelle.module.scss";

function Notifier() {
  return <span className={styles.notifier}></span>;
}

export function Notifikasjonsbjelle() {
  const { data: features } = useFeatureToggles();

  const harUlesteNotifikasjoner = true; // TODO Må utledes om man har uleste notifikasjoner fra API

  return features?.["mulighetsrommet.admin-flate-se-notifikasjoner"] ? (
    <Link to="/notifikasjoner" className={styles.lenke}>
      <div className={styles.bell_container}>
        {harUlesteNotifikasjoner ? <Notifier /> : null}
        <BellIcon fontSize={30} title="Notifikasjonsbjelle" />
      </div>
    </Link>
  ) : null;
}
