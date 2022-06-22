import React from 'react';
import './TiltaksdetaljerFane.less';
import { Tabs } from '@navikt/ds-react';
import KontaktinfoFane from './kontaktinfofane/KontaktinfoFane';
import DetaljerFane from './detaljerFane';
import { logEvent } from '../../api/logger';
import { Tiltaksgjennomforing } from '../../api/models';

interface TiltaksdetaljerFaneProps {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

const TiltaksdetaljerFane = ({ tiltaksgjennomforing }: TiltaksdetaljerFaneProps) => {
  const { tiltakstype, kontaktinfoTiltaksansvarlige, kontaktinfoArrangor } = tiltaksgjennomforing;
  const faneoverskrifter = ['For hvem', 'Detaljer og innhold', 'Påmelding og varighet', 'Kontaktinfo', 'Innsikt'];

  return (
    <Tabs
      defaultValue="tab1"
      size="medium"
      selectionFollowsFocus
      className="fane__root"
      onChange={value => logEvent('mulighetsrommet.faner', { value })}
    >
      <Tabs.List loop className="fane__liste">
        {faneoverskrifter.map((fane, index) => (
          <Tabs.Tab key={index} value={`tab${index + 1}`} label={fane} className="btn__tab" />
        ))}
      </Tabs.List>
      <Tabs.Panel value="tab1">
        <DetaljerFane
          tiltaksgjennomforingAlert={tiltaksgjennomforing.faneinnhold?.forHvemInfoboks}
          tiltakstypeAlert={tiltakstype.faneinnhold?.forHvemInfoboks}
          tiltaksgjennomforing={tiltaksgjennomforing.faneinnhold?.forHvem}
          tiltakstype={tiltakstype.faneinnhold?.forHvem}
        />
      </Tabs.Panel>
      <Tabs.Panel value="tab2">
        <DetaljerFane
          tiltaksgjennomforingAlert={tiltaksgjennomforing.faneinnhold?.detaljerOgInnholdInfoboks}
          tiltakstypeAlert={tiltakstype.faneinnhold?.detaljerOgInnholdInfoboks}
          tiltaksgjennomforing={tiltaksgjennomforing.faneinnhold?.detaljerOgInnhold}
          tiltakstype={tiltakstype.faneinnhold?.detaljerOgInnhold}
        />
      </Tabs.Panel>
      <Tabs.Panel value="tab3">
        <DetaljerFane
          tiltaksgjennomforingAlert={tiltaksgjennomforing.faneinnhold?.pameldingOgVarighetInfoboks}
          tiltakstypeAlert={tiltakstype.faneinnhold?.pameldingOgVarighetInfoboks}
          tiltaksgjennomforing={tiltaksgjennomforing.faneinnhold?.pameldingOgVarighet}
          tiltakstype={tiltakstype.faneinnhold?.pameldingOgVarighet}
        />
      </Tabs.Panel>
      <Tabs.Panel value="tab4">
        <KontaktinfoFane tiltaksansvarlige={kontaktinfoTiltaksansvarlige} arrangorinfo={kontaktinfoArrangor} />
      </Tabs.Panel>
      <Tabs.Panel value="tab5">Her kommer det grader og annet snacks - Innsikt</Tabs.Panel>
    </Tabs>
  );
};

export default TiltaksdetaljerFane;
