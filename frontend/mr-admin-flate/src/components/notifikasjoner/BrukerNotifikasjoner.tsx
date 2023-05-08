import { Heading } from "@navikt/ds-react";
import { useHentAnsatt } from "../../api/administrator/useHentAdministrator";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import styles from "./BrukerNotifikasjoner.module.scss";
import { Varsel } from "./Varsel";
import { useNotificationSummary } from "../../api/notifikasjoner/useNotificationSummary";
import { Link } from "react-router-dom";

export function BrukerNotifikasjoner() {
  const { data: features } = useFeatureToggles();
  const { data: bruker } = useHentAnsatt();
  const { data: notificationSummary } = useNotificationSummary();
  const antallUlesteNotifikasjoner = notificationSummary?.unreadCount || -1;

  if (!features?.["mulighetsrommet.admin-flate-se-notifikasjoner"]) return null;

  if (antallUlesteNotifikasjoner <= 0) return null;

  return (
    <section className={styles.container}>
      <Heading level="3" size="medium">
        Hei {bruker?.fornavn}
      </Heading>
      <Link className={styles.link} to="/notifikasjoner">
        <Varsel tittel="Varsler" melding="Du har nye varsler" />
      </Link>
    </section>
  );
}
