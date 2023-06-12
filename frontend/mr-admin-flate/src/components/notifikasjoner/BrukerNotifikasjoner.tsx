import { Heading } from "@navikt/ds-react";
import { useHentAnsatt } from "../../api/administrator/useHentAdministrator";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { useNotificationSummary } from "../../api/notifikasjoner/useNotificationSummary";
import styles from "./BrukerNotifikasjoner.module.scss";
import { Notifikasjon } from "./Notifikasjon";

export function BrukerNotifikasjoner() {
  const { data: features } = useFeatureToggles();
  const { data: bruker } = useHentAnsatt();
  const { data: notificationSummary } = useNotificationSummary();
  const antallUlesteNotifikasjoner = notificationSummary?.notDoneCount || -1;

  if (!features?.["mulighetsrommet.admin-flate-se-notifikasjoner"]) return null;

  if (antallUlesteNotifikasjoner <= 0) return null;

  return (
    <div className={styles.brukernotifikasjoner_container}>
      <Heading level="2" size="medium">
        Hei {bruker?.fornavn}
      </Heading>
      <Notifikasjon
        href="/notifikasjoner"
        tittel="Notifikasjoner"
        melding="Du har nye notifikasjoner"
      />
    </div>
  );
}
