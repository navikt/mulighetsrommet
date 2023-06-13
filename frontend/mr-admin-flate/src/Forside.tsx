import { Link } from "react-router-dom";
import styles from "./Forside.module.scss";
import { forsideKort } from "./constants";
import { BrukerNotifikasjoner } from "./components/notifikasjoner/BrukerNotifikasjoner";
import { faro } from "@grafana/faro-web-sdk";

import { BodyShort, Heading } from "@navikt/ds-react";

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
        <div className={styles.card_container}>
          {forsideKort.map((card) => (
            <Link
              key={card.url}
              className={styles.card}
              to={card.url}
              data-testid={card.navn.toLowerCase()}
              onClick={() =>
                faro?.api?.pushEvent(
                  `Bruker trykket på inngang fra forside: ${card.navn}`,
                  { inngang: card.navn },
                  "forside"
                )
              }
            >
              <span className={styles.circle}></span>
              <Heading size="medium" level="3">
                {card.navn}
              </Heading>
              <BodyShort className={styles.infotekst}>{card.tekst}</BodyShort>
            </Link>
          ))}
        </div>
      </div>
    </main>
  );
}
