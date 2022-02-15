import React from 'react';
import './Filterknapp.less';
import { useAtom } from 'jotai';
import { visSidemeny } from '../../api/atoms/atoms';
import { Button } from '@navikt/ds-react';

interface SidemenyKnappProps {
  children: React.ReactNode;
  className?: string;
}

const SidemenyKnapp = ({ children, className }: SidemenyKnappProps) => {
  const [sidemenyVisning, setSidemenyVisning] = useAtom(visSidemeny);

  return (
    <Button onClick={() => setSidemenyVisning(!sidemenyVisning)} variant="tertiary" className={className}>
      {children}
    </Button>
  );
};

export default SidemenyKnapp;
