import { Alert } from '@navikt/ds-react';
import { PortableText } from '@portabletext/react';
import styles from './Detaljfane.module.scss';

interface DetaljerFaneProps {
  tiltaksgjennomforingAlert?: string;
  tiltakstypeAlert?: string;
  tiltaksgjennomforing?: any;
  tiltakstype?: any;
}

const DetaljerFane = ({
  tiltaksgjennomforingAlert,
  tiltakstypeAlert,
  tiltaksgjennomforing,
  tiltakstype,
}: DetaljerFaneProps) => {
  return (
    <div className={styles.tiltaksdetaljer__maksbredde}>
      {tiltakstypeAlert && (
        <Alert variant="info" className={styles.tiltaksdetaljer__alert}>
          {tiltakstypeAlert}
        </Alert>
      )}
      {tiltaksgjennomforingAlert && (
        <Alert variant="info" className={styles.tiltaksdetaljer__alert}>
          {tiltaksgjennomforingAlert}
        </Alert>
      )}
      <PortableText value={tiltakstype} />
      <PortableText value={tiltaksgjennomforing} />
    </div>
  );
};

export default DetaljerFane;
