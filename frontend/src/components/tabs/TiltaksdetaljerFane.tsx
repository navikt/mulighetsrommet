import React from 'react';
import { useParams } from 'react-router-dom';
import TiltaksgjennomforingsTabell from '../../components/tabell/TiltaksgjennomforingTabell';
import useTiltaksgjennomforingerByTiltakstypeId from '../../hooks/tiltaksgjennomforing/useTiltaksgjennomforingerByTiltakstypeId';
import * as Tabs from '@radix-ui/react-tabs';
import './TiltaksdetaljerFane.less';

interface RouteParams {
  id: string;
}

const TiltaksdetaljerFane = () => {
  const params = useParams<RouteParams>();
  const id = Number(params.id);
  const tiltaksgjennomforinger = useTiltaksgjennomforingerByTiltakstypeId(id);
  const faneoverskrifter = [
    'Tiltaksgjennomføringer',
    'Om kurset',
    'Detaljer',
    'Påmelding',
    'Innhold',
    'Varighet',
    'Statistikk',
  ];

  return (
    <Tabs.Root defaultValue="tab1" orientation="vertical" className="fane__root">
      <Tabs.List className="fane__liste">
        {faneoverskrifter.map((fane, index) => (
          <Tabs.Trigger value={`tab${index + 1}`} className={`btn__tab${index + 1}`}>
            {fane}
          </Tabs.Trigger>
        ))}
      </Tabs.List>
      <Tabs.Content value="tab1">
        <TiltaksgjennomforingsTabell tiltaksgjennomforinger={tiltaksgjennomforinger.data} />
      </Tabs.Content>
      <Tabs.Content value="tab2">Om kurset</Tabs.Content>
      <Tabs.Content value="tab3">Detaljer</Tabs.Content>
      <Tabs.Content value="tab4">Påmelding</Tabs.Content>
      <Tabs.Content value="tab5">Innhold</Tabs.Content>
      <Tabs.Content value="tab6">Varighet</Tabs.Content>
      <Tabs.Content value="tab7">Statistikk</Tabs.Content>
    </Tabs.Root>
  );
};

export default TiltaksdetaljerFane;
