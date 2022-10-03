import { Heading, Ingress } from '@navikt/ds-react';
import useTiltaksgjennomforingByTiltaksnummer from '../core/api/queries/useTiltaksgjennomforingByTiltaksnummer';
import { kebabCase } from '../utils/Utils';
import styles from './TiltaksgjennomforingsHeader.module.scss';

const TiltaksgjennomforingsHeader = () => {
  const { data } = useTiltaksgjennomforingByTiltaksnummer();
  if (!data) return null;

  const { tiltaksgjennomforingNavn, beskrivelse, tiltakstype } = data;
  return (
    <>
      <Heading
        level="1"
        size="xlarge"
        className={styles.tiltaksgjennomforing__title}
        data-testid={`tiltaksgjennomforing-header_${kebabCase(tiltaksgjennomforingNavn)}`}
      >
        {tiltaksgjennomforingNavn}
      </Heading>
      {tiltakstype?.tiltakstypeNavn === 'Opplæring (Gruppe AMO)'
        ? beskrivelse && <Ingress>{beskrivelse}</Ingress>
        : null}
      {tiltakstype.beskrivelse && <Ingress>{tiltakstype.beskrivelse}</Ingress>}
    </>
  );
};

export default TiltaksgjennomforingsHeader;
