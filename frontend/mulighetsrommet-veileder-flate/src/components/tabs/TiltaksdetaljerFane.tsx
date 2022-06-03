import React from 'react';
import './TiltaksdetaljerFane.less';
import { Alert, Tabs } from '@navikt/ds-react';
import { PortableText } from '@portabletext/react';

interface TiltaksdetaljerFaneProps {
  forHvemAlert: string;
  detaljerOgInnholdAlert: string;
  pameldingOgVarighetAlert: string;
  forHvem: any;
  detaljerOgInnhold: any;
  pameldingOgVarighet: any;
  kontaktinfo: any;
}

const TiltaksdetaljerFane = ({
  forHvemAlert,
  detaljerOgInnholdAlert,
  pameldingOgVarighetAlert,
  forHvem,
  detaljerOgInnhold,
  pameldingOgVarighet,
  kontaktinfo,
}: TiltaksdetaljerFaneProps) => {
  const faneoverskrifter = ['For hvem', 'Detaljer og innhold', 'Påmelding og varighet', 'Kontaktinfo', 'Innsikt'];
  return (
    <Tabs defaultValue="tab1" size="medium" selectionFollowsFocus className="fane__root">
      <Tabs.List loop className="fane__liste">
        {faneoverskrifter.map((fane, index) => (
          <Tabs.Tab key={index} value={`tab${index + 1}`} label={fane} className="btn__tab" />
        ))}
      </Tabs.List>
      <Tabs.Panel value="tab1">
        {forHvemAlert ? <Alert variant={'info'}>{forHvemAlert}</Alert> : <></>}
        <PortableText value={forHvem} />
      </Tabs.Panel>
      <Tabs.Panel value="tab2">
        {detaljerOgInnholdAlert ? <Alert variant={'info'}>{detaljerOgInnholdAlert}</Alert> : <></>}
        <PortableText value={detaljerOgInnhold} />
      </Tabs.Panel>
      <Tabs.Panel value="tab3">
        {pameldingOgVarighetAlert ? <Alert variant={'info'}>{pameldingOgVarighetAlert}</Alert> : <></>}
        <PortableText value={pameldingOgVarighet} />
      </Tabs.Panel>
      <Tabs.Panel value="tab4">
        <PortableText value={kontaktinfo} />
      </Tabs.Panel>
      <Tabs.Panel value="tab5">Innsikt</Tabs.Panel>
    </Tabs>
  );
};

export default TiltaksdetaljerFane;
