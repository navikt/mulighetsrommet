import { Heading } from "@navikt/ds-react";
import styles from "./Forside.module.scss";
import { BrukerNotifikasjoner } from "./components/notifikasjoner/BrukerNotifikasjoner";
import { Forsidekort } from "./components/forsidekort/Forsidekort";

export function Forside() {
  return (
    <main>
      <div className={styles.hero}>
        <Heading size="large" level="2" className={styles.title}>
          Enkel og effektiv administrasjon
          <br /> av arbeidsmarkedstiltak
        </Heading>
      </div>
      <div className={styles.adminflate_container}>
        <BrukerNotifikasjoner />
        <Forsidekort />
      </div>
    </main>
  );
}
