import React from 'react';
import './TiltaksdetaljerFane.less';
import { Tabs } from '@navikt/ds-react';
import {logEvent} from "../../api/logger";

const TiltaksdetaljerFane2 = () => {
  const faneoverskrifter = ['Om kurset', 'Detaljer', 'Påmelding', 'Innhold', 'Varighet', 'Statistikk'];

  return (
    <Tabs defaultValue="tab1" size="medium" selectionFollowsFocus className="fane__root" onChange={value => logEvent('mulighetsrommet.faner', {value})}>
      <Tabs.List loop className="fane__liste">
        {faneoverskrifter.map((fane, index) => (
          <Tabs.Tab key={index} value={`tab${index + 1}`} label={fane} className="btn__tab" />
        ))}
      </Tabs.List>

      <Tabs.Panel value="tab1">Om kurset</Tabs.Panel>
      <Tabs.Panel value="tab2">Detaljer</Tabs.Panel>
      <Tabs.Panel value="tab3">Påmelding</Tabs.Panel>
      <Tabs.Panel value="tab4">Innhold</Tabs.Panel>
      <Tabs.Panel value="tab5">Varighet</Tabs.Panel>
      <Tabs.Panel value="tab6">Statistikk</Tabs.Panel>
    </Tabs>
  );
};

export default TiltaksdetaljerFane2;
