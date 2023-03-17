import { ChevronRightIcon } from "@navikt/aksel-icons";
import styles from "./Listeelementer.module.scss";
import { Link } from "react-router-dom";
import classNames from "classnames";

interface TiltaksgjennomforingRadProps {
  children: any;
  linkTo: string;
  classname?: string;
  testId?: string;
}

export function ListeRad({
  children,
  linkTo,
  classname,
  testId = "",
}: TiltaksgjennomforingRadProps) {
  return (
    <li className={styles.list_element} data-testid={testId}>
      <Link to={linkTo} className={classNames(styles.listerad, classname)}>
        {children}
        <ChevronRightIcon className={styles.pil} />
      </Link>
    </li>
  );
}
