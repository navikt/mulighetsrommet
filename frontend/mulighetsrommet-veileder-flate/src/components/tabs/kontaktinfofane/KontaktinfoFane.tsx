import React from 'react';
import { Heading } from '@navikt/ds-react';
import './KontaktinfoFane.less';
import ArrangorInfo from './ArrangorInfo';
import TiltaksansvarligInfo from './TiltaksansvarligInfo';
import { Arrangor, Tiltaksansvarlig } from '../../../core/api/models';

interface KontaktinfoFaneProps {
  tiltaksansvarlige: Tiltaksansvarlig[];
  arrangorinfo: Arrangor;
}

const KontaktinfoFane = ({ tiltaksansvarlige, arrangorinfo }: KontaktinfoFaneProps) => {
  return (
    <div className="kontaktinfo">
      <div>
        <Heading size="large" level="2" className="kontaktinfo__header">
          Arrangør
        </Heading>
        <ArrangorInfo arrangorinfo={arrangorinfo} />
      </div>
      <div>
        <Heading size="large" level="2" className="kontaktinfo__header">
          Tiltaksansvarlig
        </Heading>
        <TiltaksansvarligInfo tiltaksansvarlige={tiltaksansvarlige} />
      </div>
    </div>
  );
};

export default KontaktinfoFane;
