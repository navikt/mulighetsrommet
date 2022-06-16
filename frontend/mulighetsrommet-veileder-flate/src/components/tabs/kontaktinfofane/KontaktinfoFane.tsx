import React from 'react';
import { Heading } from '@navikt/ds-react';
import './KontaktinfoFane.less';
import ArrangorInfo from './ArrangorInfo';
import TiltaksansvarligInfo from './TiltaksansvarligInfo';
import { Arrangor, Tiltaksansvarlig } from '../../../api/models';

interface KontaktinfoFaneProps {
  tiltaksansvarligInfo: Tiltaksansvarlig;
  arrangorinfo: Arrangor;
}

const KontaktinfoFane = ({ tiltaksansvarligInfo, arrangorinfo }: KontaktinfoFaneProps) => {
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
        <TiltaksansvarligInfo tiltaksansvarlig={tiltaksansvarligInfo} />
      </div>
    </div>
  );
};

export default KontaktinfoFane;
