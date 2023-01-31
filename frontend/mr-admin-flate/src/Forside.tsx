import { Link } from "react-router-dom";
import styles from "./Forside.module.scss";

const cards = [
  {
    navn: "Tiltakstyper",
    url: "tiltakstyper",
    tekst: "Tiltakstyper er for fagansvarlige",
  },
  { navn: "Avtaler", url: "avtaler", tekst: "Avtaler er for alle" },
  {
    navn: "Gjennomføringer",
    url: "gjennomforinger",
    tekst: "Gjennomføringer er for alle",
  },
];

export function Forside() {
  return (
    <div className={styles.container}>
      <div className={styles.hero}>
        <h2 className={styles.title}>
          Enkel og effektiv administrasjon
          <br /> av arbeidsmarkedstiltak
        </h2>
      </div>
      <div className={styles.card_container}>
        <div className={styles.cards}>
          {cards.map((card) => (
            <Link key={card.url} className={styles.card} to={card.url}>
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
