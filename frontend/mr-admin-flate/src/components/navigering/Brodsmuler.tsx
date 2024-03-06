import { ArrowRightIcon } from "@navikt/aksel-icons";
import { Link } from "react-router-dom";
import styles from "./Brodsmuler.module.scss";

export interface Brodsmule {
  tittel: string;
  lenke: string;
}

interface Props {
  brodsmuler: Array<Brodsmule | undefined>;
}

function erBrodsmule(brodsmule: Brodsmule | undefined): brodsmule is Brodsmule {
  return brodsmule !== undefined;
}

export function Brodsmuler({ brodsmuler }: Props) {
  const filtrerteBrodsmuler = brodsmuler.filter(erBrodsmule);

  return (
    <nav aria-label="Brødsmulesti" className={styles.navContainer}>
      <ol className={styles.container}>
        {filtrerteBrodsmuler.filter(erBrodsmule).map((item, index) => {
          return (
            <li key={index}>
              {index > 0 && index === filtrerteBrodsmuler.length - 1 ? (
                <span>{item.tittel}</span>
              ) : (
                <div className={styles.item}>
                  <Link className={styles.link} to={item.lenke}>
                    {item.tittel}
                  </Link>
                  <ArrowRightIcon aria-label="Ikon for pil til høyre" />
                </div>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
