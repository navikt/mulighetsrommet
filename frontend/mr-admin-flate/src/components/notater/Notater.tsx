import styles from "./Notater.module.scss";
import { Button, Checkbox, Heading, Textarea } from "@navikt/ds-react";
import Notatkort from "./Notatkort";

export default function Notater() {
  return (
    <div className={styles.notater}>
      <div className={styles.notater_opprett}>
        <Textarea label={""} className={styles.notater_input} />
        <span className={styles.notater_knapp}>
          <Button>Legg til notat</Button>
        </span>
      </div>

      <div className={styles.notater_notatvegg}>
        <Heading size="medium" level="3" className={styles.notater_heading}>
          Notater
        </Heading>

        <div className={styles.notater_andrerad}>
          <Checkbox>Vis kun mine notater</Checkbox>
          Dato
        </div>

        <Notatkort />
      </div>
    </div>
  );
}
