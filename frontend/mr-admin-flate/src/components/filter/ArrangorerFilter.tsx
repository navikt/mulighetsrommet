import { Box, UNSAFE_Combobox } from "@navikt/ds-react";
import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { ArrangorKobling } from "node_modules/@tiltaksadministrasjon/api-client/build/types.gen";
import { arrangorOptions } from "@/utils/filterUtils";
import { useMemo, useCallback, memo, useState } from "react";

interface Props {
  filter: string[];
  updateFilter: (s: string[]) => void;
  arrangorKobling: ArrangorKobling;
}

export const ArrangorerFilter = memo(function ArrangorerFilter({
  filter,
  updateFilter,
  arrangorKobling,
}: Props) {
  const [searchInput, setSearchInput] = useState("");

  const { data: arrangorer } = useArrangorer(arrangorKobling, {
    sok: searchInput,
    pageSize: 100,
  });

  const options = useMemo(() => {
    if (!arrangorer?.data) return [];
    return arrangorOptions(arrangorer.data);
  }, [arrangorer?.data]);

  const handleToggleSelected = useCallback(
    (option: string, isSelected: boolean) => {
      if (isSelected) {
        updateFilter([...filter, option]);
      } else {
        updateFilter(filter.filter((v) => v !== option));
      }
    },
    [filter, updateFilter],
  );

  return (
    <Box paddingBlock="space-8">
      <UNSAFE_Combobox
        size="small"
        label="Velg arrangør"
        hideLabel
        shouldShowSelectedOptions={false}
        placeholder="Skriv inn arrangør"
        isMultiSelect
        selectedOptions={filter}
        options={options}
        onToggleSelected={handleToggleSelected}
        shouldAutocomplete
        onChange={(value) => setSearchInput(value)}
      />
    </Box>
  );
});
