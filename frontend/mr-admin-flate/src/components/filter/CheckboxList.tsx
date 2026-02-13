import { useState } from "react";
import { Box, Checkbox, Search, VStack } from "@navikt/ds-react";

interface CheckboxListProps<T> {
  items: { label: string; value: T }[];
  isChecked: (a: T) => boolean;
  onChange: (a: T) => void;
  searchable?: boolean;
  onSelectAll?: (checked: boolean) => void;
}

export function CheckboxList<T>(props: CheckboxListProps<T>) {
  const { items, isChecked, onChange, searchable = false, onSelectAll } = props;
  const [search, setSearch] = useState<string>("");

  return (
    <VStack gap="space-8">
      {searchable && (
        <Box paddingBlock="space-8 space-0" maxWidth="220px">
          <Search
            label=""
            hideLabel
            size="small"
            variant="simple"
            onChange={(v: string) => setSearch(v)}
            value={search}
          />
        </Box>
      )}
      <Box>
        {onSelectAll && (
          <Checkbox size="small" onChange={(event) => onSelectAll(event.target.checked)}>
            Velg alle
          </Checkbox>
        )}
        {items
          .filter((item) => item.label.toLocaleLowerCase().includes(search.toLocaleLowerCase()))
          .map((item) => (
            <Checkbox
              key={item.value as string}
              size="small"
              onChange={() => onChange(item.value)}
              checked={isChecked(item.value)}
            >
              {item.label}
            </Checkbox>
          ))}
      </Box>
    </VStack>
  );
}
