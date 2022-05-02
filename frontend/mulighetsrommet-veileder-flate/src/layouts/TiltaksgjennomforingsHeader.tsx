import React from 'react';
import './MainView.less';
import { Heading } from '@navikt/ds-react';
import { kebabCase } from '../utils/Utils';

interface TiltaksgjennomforingsHeaderProps {
  title: string;
  arrangor?: string;
  tiltakstype: string;
}

function TiltaksgjennomforingsHeader({ title, arrangor, tiltakstype }: TiltaksgjennomforingsHeaderProps) {
  return (
    <div className="tiltaksgjennomforing__title">
      <Heading level="1" size="xlarge" data-testid={`tiltaksgjennomforing-header_${kebabCase(title)}`}>
        {title}
      </Heading>
      <div className="tiltaksgjennomforing__subtitle-container">
        {arrangor && (
          <Heading level="2" size="xsmall">
            {arrangor}
          </Heading>
        )}
        <Heading level="2" size="xsmall">
          {tiltakstype}
        </Heading>
      </div>
    </div>
  );
}

export default TiltaksgjennomforingsHeader;
