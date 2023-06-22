import { Heading, Ingress } from '@navikt/ds-react';
import useTiltaksgjennomforingById from '../core/api/queries/useTiltaksgjennomforingById';
import { kebabCase } from '../utils/Utils';
import styles from './TiltaksgjennomforingsHeader.module.scss';
import { SanityTiltakstype } from 'mulighetsrommet-api-client';

const TiltaksgjennomforingsHeader = () => {
  const { data } = useTiltaksgjennomforingById();
  if (!data) return null;

  const { tiltaksgjennomforingNavn, beskrivelse, tiltakstype } = data;
  return (
    <>
      <Heading
        level="1"
        size="xlarge"
        className={styles.tiltaksgjennomforing_title}
        data-testid={`tiltaksgjennomforing-header_${kebabCase(tiltaksgjennomforingNavn)}`}
      >
        {tiltaksgjennomforingNavn}
      </Heading>
      {tiltakstype?.arenakode === SanityTiltakstype.arenakode.GRUPPEAMO
        ? beskrivelse && <Ingress>{beskrivelse}</Ingress>
        : null}
      {tiltakstype.beskrivelse && <Ingress className={styles.beskrivelse}>{tiltakstype.beskrivelse}</Ingress>}
    </>
  );
};

export default TiltaksgjennomforingsHeader;
