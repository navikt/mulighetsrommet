import { Alert } from '@navikt/ds-react';
import { PortableText } from '@portabletext/react';
import styles from './Detaljerfane.module.scss';
import FaneTiltaksinformasjon from './FaneTiltaksinformasjon';

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
    <FaneTiltaksinformasjon
      harInnhold={tiltaksgjennomforingAlert || tiltakstypeAlert || tiltaksgjennomforing || tiltakstype}
    >
      {tiltakstypeAlert && (
        <Alert variant="info" className={styles.tiltaksdetaljer_alert}>
          {tiltakstypeAlert}
        </Alert>
      )}
      {tiltaksgjennomforingAlert && (
        <Alert variant="info" className={styles.tiltaksdetaljer_alert}>
          {tiltaksgjennomforingAlert}
        </Alert>
      )}
      <PortableText value={tiltakstype} />
      <PortableText value={tiltaksgjennomforing} />
    </FaneTiltaksinformasjon>
  );
};

export default DetaljerFane;
