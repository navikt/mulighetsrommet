import React from 'react';
import './TiltaksgjennomforingsHeader.less';
import { Heading } from '@navikt/ds-react';
import { kebabCase } from '../utils/Utils';
import { PortableText } from '@portabletext/react';

interface TiltaksgjennomforingsHeaderProps {
  tiltaksgjennomforingsnavn: string;
  beskrivelseTiltaksgjennomforing?: string;
  beskrivelseTiltakstype?: any;
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
      <div className="tiltaksgjennomforing__beskrivelse">
        <PortableText value={beskrivelseTiltakstype} />
      </div>
    </div>
  );
};

export default TiltaksgjennomforingsHeader;
