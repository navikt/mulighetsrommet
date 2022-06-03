import React from 'react';
import './TiltaksgjennomforingsHeader.less';
import { Heading } from '@navikt/ds-react';
import { kebabCase } from '../utils/Utils';

interface TiltaksgjennomforingsHeaderProps {
  tiltaksgjennomforingsnavn: string;
  beskrivelse: string;
}

function TiltaksgjennomforingsHeader({ tiltaksgjennomforingsnavn, beskrivelse }: TiltaksgjennomforingsHeaderProps) {
  return (
    <div className="tiltaksgjennomforing__title">
      <Heading
        level="1"
        size="xlarge"
        data-testid={`tiltaksgjennomforing-header_${kebabCase(tiltaksgjennomforingsnavn)}`}
      >
        {tiltaksgjennomforingsnavn}
      </Heading>
      <div className="tiltaksgjennomforing__beskrivelse">{beskrivelse}</div>
    </div>
  );
}

export default TiltaksgjennomforingsHeader;
