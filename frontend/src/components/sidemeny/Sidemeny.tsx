import { Heading, Panel } from '@navikt/ds-react';
import React from 'react';
import './Sidemeny.less';

interface SidemenyProps {
  children?: React.ReactNode;
}
const Sidemeny = ({ children }: SidemenyProps) => {
  return (
    <Panel border className="sidemeny">
      <Heading level="3" size="medium">
        Meny
      </Heading>
      {children}
    </Panel>
  );
};

export default Sidemeny;
