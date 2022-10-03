import React from 'react';
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
      aria-label="Søk etter tiltakstype"
      data-testid="filter_sokefelt"
    />
  );
};

export default Searchfield;
