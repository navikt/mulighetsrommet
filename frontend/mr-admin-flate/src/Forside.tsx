import { Link } from "react-router-dom";
import { forsideKort } from "./constants";
import styles from "./Forside.module.scss";

export function Forside() {
  return (
    <div role="main" className={styles.container}>
      <div className={styles.hero}>
        <h2 className={styles.title}>
          Enkel og effektiv administrasjon
          <br /> av arbeidsmarkedstiltak
        </h2>
      </div>
      <div className={styles.card_container}>
        <div className={styles.cards}>
          {forsideKort.map((card) => (
            <Link
              key={card.url}
              className={styles.card}
              to={card.url}
              data-testid={card.navn.toLowerCase()}
            >
              <span className={styles.circle}></span>
              <h3>{card.navn}</h3>
              <p className={styles.infotekst}>{card.tekst}</p>
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}
