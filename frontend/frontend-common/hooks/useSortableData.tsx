import { useMemo, useState } from "react";
import { compare } from "../utils/utils";
import { SortState } from "@navikt/ds-react";

interface ScopedSortState<T> extends SortState {
  orderBy: Extract<keyof T, string>; // Ensures string keys only
  direction: "ascending" | "descending";
}

export function useSortableData<T extends Record<string, unknown>>(
  data: T[],
  defaultSort?: ScopedSortState<T>,
  comparator: (
    a: T[Extract<keyof T, unknown>],
    b: T[Extract<keyof T, unknown>],
  ) => number = compare,
) {
  const [sort, setSort] = useState<ScopedSortState<T> | undefined>(defaultSort);

  const sortedData = useMemo(() => {
    if (!sort) return data;

    const { orderBy, direction } = sort;
    return [...data].sort((a, b) => {
      const aVal = a[orderBy];
      const bVal = b[orderBy];

      return direction === "ascending" ? comparator(bVal, aVal) : comparator(aVal, bVal);
    });
  }, [data, sort]);

  const toggleSort = (key: Extract<keyof T, string>) => {
    setSort((prev): ScopedSortState<T> | undefined => {
      if (!prev || prev.orderBy !== key) {
        return { orderBy: key, direction: "ascending" };
      }
      if (prev.direction === "ascending") {
        return { orderBy: key, direction: "descending" };
      }
      return undefined;
    });
  };

  return { sortedData, sort, setSort, toggleSort };
}
