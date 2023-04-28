import { BodyShort, Heading } from "@navikt/ds-react";
import { BellIcon, ChevronRightIcon } from "@navikt/aksel-icons";
import styles from "./Varsel.module.scss";

interface IVarsel {
  tittel: string;
  melding: string;
}

export function Varsel({ tittel, melding }: IVarsel) {
  return (
    <div className={styles.container}>
      <div className={styles.left}>
        <div>
          <span className={styles.ikon_container}>
            <BellIcon className={styles.ikon} />
          </span>
        </div>
        <div>
          <Heading size="small" level="3">
            {tittel}
          </Heading>
          <BodyShort>{melding}</BodyShort>
        </div>
      </div>
      <div>
        <ChevronRightIcon />
      </div>
    </div>
  );
}
