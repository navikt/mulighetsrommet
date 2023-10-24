import { faro } from "@grafana/faro-web-sdk";
import { FileCheckmarkIcon, HandshakeIcon, TokenIcon } from "@navikt/aksel-icons";
import { BodyShort, Heading } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import styles from "./Forside.module.scss";
import { BrukerNotifikasjoner } from "./components/notifikasjoner/BrukerNotifikasjoner";
import { forsideKort } from "./constants";

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
                  {
                    inngang: card.navn,
                  },
                  "forside",
                )
              }
            >
              <span className={styles.circle}>
                {card.url === "avtaler" ? (
                  <HandshakeIcon />
                ) : card.url === "tiltakstyper" ? (
                  <TokenIcon />
                ) : card.url.includes("sanity") ? (
                  <img src="./sanity_logo.png" alt="Sanity-logo" />
                ) : (
                  <FileCheckmarkIcon />
                )}
              </span>
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
