import { Search } from "@navikt/ds-react";
import { WritableAtom, useAtom } from "jotai";
import { ArrangorerFilter as ArrangorerFilterProps } from "../../api/atoms";

interface Props {
  filterAtom: WritableAtom<ArrangorerFilterProps, [newValue: ArrangorerFilterProps], void>;
}

export function ArrangorerFilter({ filterAtom }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);

  return (
    <div>
      <Search
        label="Søk etter arrangør"
        hideLabel
        size="small"
        variant="simple"
        placeholder="Navn, organisasjonsnummer"
        onChange={(search: string) => {
          setFilter({ ...filter, sok: search });
        }}
        value={filter.sok}
        aria-label="Søk etter arrangør"
      />
    </div>
  );
}
