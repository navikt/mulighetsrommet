import React, { useState } from 'react';
import { Accordion } from '@navikt/ds-react';
import './Sidemeny.less';

interface SidemenyAccordionProps {
  tittel: string;
  children: React.ReactNode;
  isOpen: boolean;
}

const SidemenyAccordion = ({ tittel, children, isOpen }: SidemenyAccordionProps) => {
  const [open, setOpen] = useState<boolean>(isOpen);

  return (
    <Accordion className="sidemeny-accordion">
      <Accordion.Item open={open}>
        <Accordion.Header onClick={() => setOpen(!open)}>{tittel}</Accordion.Header>
        <Accordion.Content>{children}</Accordion.Content>
      </Accordion.Item>
    </Accordion>
  );
};

export default SidemenyAccordion;
