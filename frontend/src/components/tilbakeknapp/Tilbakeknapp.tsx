import { Button } from '@navikt/ds-react';
import { Back } from '@navikt/ds-icons';
import React from 'react';
import { useHistory } from 'react-router-dom';
import './Tilbakeknapp.less';

interface TilbakeknappProps {
  tilbakelenke?: string;
}

const Tilbakeknapp = ({ tilbakelenke }: TilbakeknappProps) => {
  const history = useHistory();
  return tilbakelenke ? (
    <Button
      className="tilbakeknapp"
      variant="tertiary"
      data-testid="tilbakeknapp"
      onClick={() => tilbakelenke && history.push(tilbakelenke)}
    >
      <Back aria-label="Tilbakeknapp" />
      <span>Tilbake</span>
    </Button>
  ) : (
    <></>
  );
};
export default Tilbakeknapp;
