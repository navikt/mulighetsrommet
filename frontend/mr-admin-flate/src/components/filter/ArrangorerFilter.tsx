import { Search } from "@navikt/ds-react";
import { ArrangorerFilterType } from "@/pages/arrangor/filter";

interface Props {
  filter: ArrangorerFilterType;
  updateFilter: (values: Partial<ArrangorerFilterType>) => void;
}

export function ArrangorerFilter({ filter, updateFilter }: Props) {
  return (
    <div>
      <Search
        label="Søk etter arrangør"
        hideLabel
        size="small"
        variant="simple"
        placeholder="Navn, organisasjonsnummer"
        onChange={(search: string) => {
          updateFilter({ sok: search, page: 1 });
        }}
        value={filter.sok}
        aria-label="Søk etter arrangør"
      />
    </div>
  );
}
