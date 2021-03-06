import { Back } from '@navikt/ds-icons';
import React from 'react';
import './Tilbakeknapp.less';
import Lenke from '../lenke/Lenke';

interface TilbakeknappProps {
  tilbakelenke: string;
}

const Tilbakeknapp = ({ tilbakelenke }: TilbakeknappProps) => {
  return (
    <Lenke className="tilbakeknapp" to={tilbakelenke} data-testid="tilbakeknapp">
      <Back aria-label="Tilbakeknapp" />
      <span>Tilbake</span>
    </Lenke>
  );
};
export default Tilbakeknapp;
