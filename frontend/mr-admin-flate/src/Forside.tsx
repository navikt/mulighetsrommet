import { Heading } from "@navikt/ds-react";
import styles from "./Forside.module.scss";
import { BrukerNotifikasjoner } from "./components/notifikasjoner/BrukerNotifikasjoner";
import { ForsidekortListe } from "./components/forsidekort/ForsidekortListe";
import { useTitle } from "@mr/frontend-common";

export function Forside() {
  useTitle("NAV Tiltaksadministrasjon");
  return (
    <main>
      <div className={styles.hero}>
        <Heading size="large" level="2" className={styles.title} data-testid="heading">
          Enkel og effektiv administrasjon
          <br /> av arbeidsmarkedstiltak
        </Heading>
      </div>
      <div className={styles.adminflate_container}>
        <BrukerNotifikasjoner />
        <ForsidekortListe />
      </div>
    </main>
  );
}
