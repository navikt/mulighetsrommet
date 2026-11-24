import { DataGrid } from "@navikt/ds-react/PREVIEW";
import { useMemo, useState } from "react";

export type SortComparators<T> = Partial<Record<string, (a: T, b: T) => number>>;

export function useDataGridSort<T>(
  data: T[],
  comparators: SortComparators<T>,
  initialSortOrder?: NonNullable<DataGrid.Table.Sorting["sortOrder"]>,
) {
  const [sortState, setSortState] = useState<NonNullable<DataGrid.Table.Sorting["sortOrder"]>>(
    initialSortOrder ?? [],
  );

  const handleSortOrderChange: DataGrid.Table.Sorting["onSortOrderChange"] = (newSortOrder) => {
    setSortState(newSortOrder);
  };

  const sortedData = useMemo(() => {
    if (sortState.length === 0) {
      return data;
    }

    return data.toSorted((a, b) => {
      for (const sort of sortState) {
        const compare = comparators[sort.columnId];

        if (!compare) {
          continue;
        }

        const comparison = compare(a, b);

        if (comparison !== 0) {
          return sort.direction === "asc" ? comparison : -comparison;
        }
      }

      return 0;
    });
  }, [data, comparators, sortState]);

  return {
    sortedData,
    sortState,
    handleSortOrderChange,
  };
}
