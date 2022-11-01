import React from 'react';
import { TextField } from '@navikt/ds-react';
import styles from './Filtermeny.module.scss';

interface SokeFilterProps {
  sokefilter: string;
  setSokefilter: (sokefilter: string) => void;
}

const Sokefelt = ({ sokefilter, setSokefilter }: SokeFilterProps) => {
  return (
    <TextField
      label=""
      placeholder="Søk etter tiltak"
      hideLabel
      onChange={(e: React.FormEvent<HTMLInputElement>) => setSokefilter(e.currentTarget.value)}
      value={sokefilter}
      className={styles.sokefelt}
      aria-label="Søk etter tiltak"
      data-testid="filter_sokefelt"
    />
  );
};

export default Sokefelt;
