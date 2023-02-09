import { Next } from "@navikt/ds-icons";
import styles from "./Listeelementer.module.scss";
import { Link } from "react-router-dom";
import classNames from "classnames";

interface TiltaksgjennomforingRadProps {
  children: any;
  linkTo: string;
  classname?: string;
}

export function ListeRad({
  children,
  linkTo,
  classname,
}: TiltaksgjennomforingRadProps) {
  return (
    <li className={styles.list_element} data-testid="tiltakstyperad">
      <Link to={linkTo} className={classNames(styles.listerad, classname)}>
        {children}
        <Next className={styles.pil} />
      </Link>
    </li>
  );
}
