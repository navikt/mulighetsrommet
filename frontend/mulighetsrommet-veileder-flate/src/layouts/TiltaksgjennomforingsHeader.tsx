import React, { useEffect, useState } from 'react';
import './MainView.less';
import { Heading } from '@navikt/ds-react';
import { kebabCase } from '../utils/Utils';
import { sanityClient } from '../sanityClient';

interface TiltaksgjennomforingsHeaderProps {
  tiltakstype: string;
  arrangor?: string;
}

function TiltaksgjennomforingsHeader({ tiltakstype, arrangor }: TiltaksgjennomforingsHeaderProps) {
  const [gjennomforing, setGjennomforing] = useState(null);

  useEffect(() => {
    sanityClient
      .fetch(`*[_type == "tiltaksgjennomforing"]`)
      .then(data => setGjennomforing(data))
      .catch(console.error);
  }, []);

  // console.log('gjennomføring', gjennomforing);

  return (
    <div className="tiltaksgjennomforing__title">
      <Heading level="1" size="xlarge" data-testid={`tiltaksgjennomforing-header_${kebabCase(tiltakstype)}`}>
        {tiltakstype}
        {/*{gjennomforing ? gjennomforing.title : 'Teest'}*/}
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
