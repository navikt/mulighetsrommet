import { Link } from "react-router-dom";
import styles from "./Forside.module.scss";
import { forsideKort } from "./constants";
import { BrukerNotifikasjoner } from "./components/notifikasjoner/BrukerNotifikasjoner";
import { faro } from "@grafana/faro-web-sdk";

export function Forside() {
  return (
    <main>
      <div className={styles.hero}>
        <h2 className={styles.title}>
          Enkel og effektiv administrasjon
          <br /> av arbeidsmarkedstiltak
        </h2>
      </div>
      <div className={styles.container}>
        <BrukerNotifikasjoner />
        <div className={styles.card_container}>
          <div className={styles.cards}>
            {forsideKort.map((card) => (
              <Link
                key={card.url}
                className={styles.card}
                to={card.url}
                data-testid={card.navn.toLowerCase()}
                onClick={() =>
                  faro?.api?.pushEvent(
                    "Bruker trykket på inngang fra forside",
                    { inngang: card.navn },
                    "forside"
                  )
                }
              >
                <span className={styles.circle}></span>
                <h3>{card.navn}</h3>
                <p className={styles.infotekst}>{card.tekst}</p>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </main>
  );
}
