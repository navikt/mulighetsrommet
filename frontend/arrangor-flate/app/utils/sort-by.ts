import { useState } from "react";

export type SortOrder = "ascending" | "descending";

export type SortBySelector<T> = (a: T) => string | number | boolean | undefined | null;

export interface SortState<TKey> {
  orderBy: TKey;
  direction: SortOrder;
}

export function sortBy<T extends object>(
  array: T[],
  order: SortOrder,
  selector: SortBySelector<T>,
): T[] {
  return array.slice().sort((a, b) => {
    const valueA = selector(a);
    const valueB = selector(b);

    return order === "ascending" ? comparator(valueA, valueB) : comparator(valueB, valueA);
  });
}

function comparator<T>(a: T, b: T): number {
  if (b == null || b < a) {
    return -1;
  }
  if (b > a) {
    return 1;
  }
  return 0;
}

export function useSortState<TKey extends string>() {
  const [sort, setSort] = useState<SortState<TKey> | undefined>();

  const handleSort = (clickedKey: TKey) => {
    if (sort?.orderBy === clickedKey) {
      if (sort.direction === "ascending") {
        setSort({ orderBy: clickedKey, direction: "descending" });
      } else {
        setSort(undefined);
      }
    } else {
      setSort({ orderBy: clickedKey, direction: "ascending" });
    }
  };

  return { sort, setSort, handleSort };
}
