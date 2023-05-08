import { Heading } from "@navikt/ds-react";
import { useHentAnsatt } from "../../api/administrator/useHentAdministrator";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { useNotificationSummary } from "../../api/notifikasjoner/useNotificationSummary";
import styles from "./BrukerNotifikasjoner.module.scss";
import { Varsel } from "./Varsel";

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
      <Varsel
        href="/notifikasjoner"
        tittel="Varsler"
        melding="Du har nye varsler"
      />
    </section>
  );
}
