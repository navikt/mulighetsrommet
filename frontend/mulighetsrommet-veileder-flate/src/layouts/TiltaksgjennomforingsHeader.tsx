import { BodyLong, Heading } from '@navikt/ds-react';
import { VeilederflateTiltaksgjennomforing, VeilederflateTiltakstype } from 'mulighetsrommet-api-client';
import { kebabCase } from '../utils/Utils';
import styles from './TiltaksgjennomforingsHeader.module.scss';

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
}

const TiltaksgjennomforingsHeader = ({ tiltaksgjennomforing }: Props) => {
  const { tiltaksgjennomforingNavn, beskrivelse, tiltakstype } = tiltaksgjennomforing;
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
      {tiltakstype?.arenakode === VeilederflateTiltakstype.arenakode.GRUPPEAMO
        ? beskrivelse && <BodyLong>{beskrivelse}</BodyLong>
        : null}
      {tiltakstype.beskrivelse && (
        <BodyLong size="large" className={styles.beskrivelse}>
          {tiltakstype.beskrivelse}
        </BodyLong>
      )}
    </>
  );
};

export default TiltaksgjennomforingsHeader;
