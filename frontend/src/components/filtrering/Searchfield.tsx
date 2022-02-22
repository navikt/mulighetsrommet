import React from 'react';
import '../../views/tiltakstype-oversikt/ViewTiltakstypeOversikt.less';
import { TextField } from '@navikt/ds-react';

interface SokeFilterProps {
  sokefilter: string;
  setSokefilter: (sokefilter: string) => void;
}

const Searchfield = ({ sokefilter, setSokefilter }: SokeFilterProps) => {
  return (
    <TextField
      label="Søk etter tiltakstype:"
      hideLabel
      onChange={(e: React.FormEvent<HTMLInputElement>) => setSokefilter(e.currentTarget.value)}
      value={sokefilter}
      data-testid="sokefelt_tiltakstype"
      className="sokefelt-tiltakstype"
      aria-label="Søk etter tiltakstype"
    />
  );
};

export default Searchfield;
