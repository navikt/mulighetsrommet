import { WritableAtom, useAtom } from "jotai";
import { ArrangorerFilter as ArrangorerFilterProps } from "../../api/atoms";
import { Search, Skeleton, VStack } from "@navikt/ds-react";

interface Props {
  filterAtom: WritableAtom<ArrangorerFilterProps, [newValue: ArrangorerFilterProps], void>;
}

export function ArrangorerFilter({ filterAtom }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);

  if (filter.sok.length > 0) {
    return (
      <VStack gap="2">
        <Skeleton height={50} variant="rounded" />
      </VStack>
    );
  }

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
