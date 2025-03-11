import { Search } from "@navikt/ds-react";

interface SokeFilterProps {
  sokefilter: string;
  setSokefilter: (sokefilter: string) => void;
}

const Sokefelt = ({ sokefilter, setSokefilter }: SokeFilterProps) => {
  return (
    <Search
      label=""
      placeholder="Søk etter tiltak"
      hideLabel
      variant="simple"
      onChange={(e: string) => setSokefilter(e)}
      value={sokefilter}
      aria-label="Søk etter tiltak"
      data-testid="filter_sokefelt"
      size="small"
      maxLength={50}
    />
  );
};

export default Sokefelt;
