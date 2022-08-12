import React from 'react';
import './TiltaksgjennomforingsHeader.less';
import { Heading } from '@navikt/ds-react';
import { kebabCase } from '../utils/Utils';
import useTiltaksgjennomforingByTiltaksnummer from '../core/api/queries/useTiltaksgjennomforingByTiltaksnummer';

const TiltaksgjennomforingsHeader = () => {
  const { data } = useTiltaksgjennomforingByTiltaksnummer();
  if (!data) return null;

  const { tiltaksgjennomforingNavn, beskrivelse, tiltakstype } = data;
  return (
    <div className="tiltaksgjennomforing__title">
      <Heading
        level="1"
        size="xlarge"
        data-testid={`tiltaksgjennomforing-header_${kebabCase(tiltaksgjennomforingNavn)}`}
      >
        {tiltaksgjennomforingNavn}
      </Heading>
      {tiltakstype?.tiltakstypeNavn === 'Opplæring (Gruppe AMO)'
        ? beskrivelse && <div className="tiltaksgjennomforing__beskrivelse">{beskrivelse}</div>
        : null}

      {tiltakstype.beskrivelse && <div className="tiltaksgjennomforing__beskrivelse">{tiltakstype.beskrivelse}</div>}
    </div>
  );
};

export default TiltaksgjennomforingsHeader;
