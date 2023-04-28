import { Heading } from "@navikt/ds-react";
import { useHentAnsatt } from "../../api/administrator/useHentAdministrator";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import styles from "./BrukerNotifikasjoner.module.scss";
import { Varsel } from "./Varsel";

export function BrukerNotifikasjoner() {
  const { data: features } = useFeatureToggles();
  const { data: bruker } = useHentAnsatt();

  if (!features?.["mulighetsrommet.admin-flate-se-notifikasjoner"]) return null;

  return (
    <section className={styles.container}>
      <Heading level="3" size="medium">
        Hei {bruker?.fornavn}
      </Heading>
      <Varsel
        tittel="Varsler"
        melding="Her kommer det funksjonalitet for varsler"
      />
    </section>
  );
}
