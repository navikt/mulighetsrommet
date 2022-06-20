import React from 'react';
import './TiltaksgjennomforingsHeader.less';
import { Heading } from '@navikt/ds-react';
import { kebabCase } from '../utils/Utils';
import { Tiltaksgjennomforing } from '../api/models';

interface TiltaksgjennomforingsHeaderProps {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

const TiltaksgjennomforingsHeader = ({ tiltaksgjennomforing }: TiltaksgjennomforingsHeaderProps) => {
  const { tiltaksgjennomforingNavn, beskrivelse, tiltakstype } = tiltaksgjennomforing;
  return (
    <div className="tiltaksgjennomforing__title">
      <Heading
        level="1"
        size="xlarge"
        data-testid={`tiltaksgjennomforing-header_${kebabCase(tiltaksgjennomforingNavn)}`}
      >
        {tiltaksgjennomforingNavn}
      </Heading>
      {beskrivelse && <div className="tiltaksgjennomforing__beskrivelse">{beskrivelse}</div>}
      {tiltakstype.beskrivelse && <div className="tiltaksgjennomforing__beskrivelse">{tiltakstype.beskrivelse}</div>}
    </div>
  );
};

export default TiltaksgjennomforingsHeader;
