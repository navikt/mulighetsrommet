import { Heading } from '@navikt/ds-react';
import { Arrangor, Tiltaksansvarlig } from '../../../core/api/models';
import ArrangorInfo from './ArrangorInfo';
import './KontaktinfoFane.less';
import TiltaksansvarligInfo from './TiltaksansvarligInfo';

interface KontaktinfoFaneProps {
  tiltaksansvarlige: Tiltaksansvarlig[];
  arrangorinfo?: Arrangor;
}

const KontaktinfoFane = ({ tiltaksansvarlige, arrangorinfo }: KontaktinfoFaneProps) => {
  return (
    <div className="kontaktinfo">
      <div>
        <Heading size="large" level="2" className="kontaktinfo__header">
          Arrangør
        </Heading>
        {arrangorinfo ? <ArrangorInfo arrangorinfo={arrangorinfo} /> : null}
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
