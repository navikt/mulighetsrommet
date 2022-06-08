import React from 'react';
import './TiltaksgjennomforingsHeader.less';
import { Heading } from '@navikt/ds-react';
import { kebabCase } from '../utils/Utils';

interface TiltaksgjennomforingsHeaderProps {
  tiltaksgjennomforingsnavn: string;
  beskrivelseTiltaksgjennomforing?: string;
  beskrivelseTiltakstype?: string;
}

const TiltaksgjennomforingsHeader = ({
  tiltaksgjennomforingsnavn,
  beskrivelseTiltaksgjennomforing,
  beskrivelseTiltakstype,
}: TiltaksgjennomforingsHeaderProps) => {
  return (
    <div className="tiltaksgjennomforing__title">
      <Heading
        level="1"
        size="xlarge"
        data-testid={`tiltaksgjennomforing-header_${kebabCase(tiltaksgjennomforingsnavn)}`}
      >
        {tiltaksgjennomforingsnavn}
      </Heading>
      {beskrivelseTiltaksgjennomforing && (
        <div className="tiltaksgjennomforing__beskrivelse">{beskrivelseTiltaksgjennomforing}</div>
      )}
      {beskrivelseTiltakstype && <div className="tiltaksgjennomforing__beskrivelse">{beskrivelseTiltakstype}</div>}
    </div>
  );
};

export default TiltaksgjennomforingsHeader;
