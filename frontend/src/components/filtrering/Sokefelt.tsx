import React from 'react';
import '../../views/tiltakstype-oversikt/ViewTiltakstypeOversikt.less';
import { TextField } from '@navikt/ds-react';

interface SokeFilterProps {
  sokefilter: string;
  setSokefilter: (sokefilter: string) => void;
}

const Sokefelt = ({ sokefilter, setSokefilter }: SokeFilterProps) => {
  return (
    <TextField
      label="Søk etter tiltakstype:"
      hideLabel
      onChange={e => setSokefilter(e.currentTarget.value)}
      value={sokefilter}
      data-testid="sokefelt_tiltakstype"
      className="sokefelt-tiltakstype"
    />
  );
};

export default Sokefelt;
