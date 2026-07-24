import { Search } from "@navikt/ds-react";

interface SokeFilterProps {
  sokefilter: string;
  setSokefilter: (sokefilter: string) => void;
}

const Sokefelt = ({ sokefilter, setSokefilter }: SokeFilterProps) => {
  return (
    <Search
      label="Søk etter tiltak"
      placeholder="Søk etter tiltak"
      variant="secondary"
      onChange={(e: string) => setSokefilter(e)}
      value={sokefilter}
      data-testid="filter_sokefelt"
      size="small"
      maxLength={50}
    />
  );
};

export default Sokefelt;
