import React from 'react';
import './TiltaksdetaljerFane.less';
import { Tabs } from '@navikt/ds-react';
import KontaktinfoFane from './kontaktinfofane/KontaktinfoFane';
import DetaljerFane from './detaljerFane';
import { logEvent } from '../../core/api/logger';
import useTiltaksgjennomforingByTiltaksnummer from '../../core/api/queries/useTiltaksgjennomforingByTiltaksnummer';
import { kebabCase } from '../../utils/Utils';
import { useAtom } from 'jotai';
import { faneAtom } from '../../core/atoms/atoms';
import InnsiktsFane from './innsikt/InnsiktsFane';

const TiltaksdetaljerFane = () => {
  const { data } = useTiltaksgjennomforingByTiltaksnummer();
  const [fane, setFane] = useAtom(faneAtom);
  if (!data) return null;

  const { tiltakstype, kontaktinfoTiltaksansvarlige, kontaktinfoArrangor, faneinnhold } = data;
  const faneoverskrifter = ['For hvem', 'Detaljer og innhold', 'Påmelding og varighet', 'Kontaktinfo', 'Innsikt'];
  const tabValueTilFaneoverSkrifter: { [key: string]: string } = {
    tab1: faneoverskrifter[0],
    tab2: faneoverskrifter[1],
    tab3: faneoverskrifter[2],
    tab4: faneoverskrifter[3],
    tab5: faneoverskrifter[4],
  };

  return (
    <Tabs
      defaultValue={fane}
      size="medium"
      selectionFollowsFocus
      className="fane__root"
      onChange={value => {
        logEvent('mulighetsrommet.faner', { value: tabValueTilFaneoverSkrifter[value] ?? value });
        setFane(value);
      }}
    >
      <Tabs.List loop className="fane__liste">
        {faneoverskrifter.map((fane, index) => (
          <Tabs.Tab
            key={index}
            value={`tab${index + 1}`}
            label={fane}
            className="btn__tab"
            data-testid={`fane_${kebabCase(fane)}`}
          />
        ))}
      </Tabs.List>
      <Tabs.Panel value="tab1" data-testid="tab1">
        <DetaljerFane
          tiltaksgjennomforingAlert={faneinnhold?.forHvemInfoboks}
          tiltakstypeAlert={tiltakstype.faneinnhold?.forHvemInfoboks}
          tiltaksgjennomforing={faneinnhold?.forHvem}
          tiltakstype={tiltakstype.faneinnhold?.forHvem}
        />
      </Tabs.Panel>
      <Tabs.Panel value="tab2" data-testid="tab2">
        <DetaljerFane
          tiltaksgjennomforingAlert={faneinnhold?.detaljerOgInnholdInfoboks}
          tiltakstypeAlert={tiltakstype.faneinnhold?.detaljerOgInnholdInfoboks}
          tiltaksgjennomforing={faneinnhold?.detaljerOgInnhold}
          tiltakstype={tiltakstype.faneinnhold?.detaljerOgInnhold}
        />
      </Tabs.Panel>
      <Tabs.Panel value="tab3" data-testid="tab3">
        <DetaljerFane
          tiltaksgjennomforingAlert={faneinnhold?.pameldingOgVarighetInfoboks}
          tiltakstypeAlert={tiltakstype.faneinnhold?.pameldingOgVarighetInfoboks}
          tiltaksgjennomforing={faneinnhold?.pameldingOgVarighet}
          tiltakstype={tiltakstype.faneinnhold?.pameldingOgVarighet}
        />
      </Tabs.Panel>
      <Tabs.Panel value="tab4" data-testid="tab4">
        <KontaktinfoFane tiltaksansvarlige={kontaktinfoTiltaksansvarlige} arrangorinfo={kontaktinfoArrangor} />
      </Tabs.Panel>
      <Tabs.Panel value="tab5" data-testid="tab5">
        <InnsiktsFane tiltakstype={tiltakstype.tiltakstypeNavn} />
      </Tabs.Panel>
    </Tabs>
  );
};

export default TiltaksdetaljerFane;
