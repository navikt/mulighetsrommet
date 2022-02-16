import React from 'react';
import './Sidemenyknapp.less';
import { useAtom } from 'jotai';
import { visSidemeny } from '../../api/atoms/atoms';
import { Button } from '@navikt/ds-react';

interface SidemenyKnappProps {
  children: React.ReactNode;
  className?: string;
}

const SidemenyKnapp = ({ children, className }: SidemenyKnappProps) => {
  const [sidemenyVisning, setSidemenyVisning] = useAtom(visSidemeny);

  const handleClick = () => {
    setSidemenyVisning(!sidemenyVisning);
    sidemenyVisning
      ? (document.getElementById('tiltakstype-oversikt')!.style.gridTemplateColumns = 'auto')
      : (document.getElementById('tiltakstype-oversikt')!.style.gridTemplateColumns = '15rem auto');
  };

  return (
    <Button onClick={handleClick} variant="tertiary" className={className}>
      {children}
    </Button>
  );
};

export default SidemenyKnapp;
