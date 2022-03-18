import React from 'react';
import * as Tabs from '@radix-ui/react-tabs';
import './TiltaksdetaljerFane.less';

const TiltaksdetaljerFane = () => {
  const faneoverskrifter = ['Om kurset', 'Detaljer', 'Påmelding', 'Innhold', 'Varighet', 'Statistikk'];

  return (
    <Tabs.Root defaultValue="tab1" orientation="vertical" className="fane__root">
      <Tabs.List className="fane__liste">
        {faneoverskrifter.map((fane, index) => (
          <Tabs.Trigger key={index} value={`tab${index + 1}`} className="btn__tab">
            {fane}
          </Tabs.Trigger>
        ))}
      </Tabs.List>
      <Tabs.Content value="tab1">Om kurset</Tabs.Content>
      <Tabs.Content value="tab2">Detaljer</Tabs.Content>
      <Tabs.Content value="tab3">Påmelding</Tabs.Content>
      <Tabs.Content value="tab4">Innhold</Tabs.Content>
      <Tabs.Content value="tab5">Varighet</Tabs.Content>
      <Tabs.Content value="tab6">Statistikk</Tabs.Content>
    </Tabs.Root>
  );
};

export default TiltaksdetaljerFane;
