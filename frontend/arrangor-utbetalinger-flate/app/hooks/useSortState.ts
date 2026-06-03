import { useState } from "react";
import { SortOrder } from "~/utils/sort-by";

export interface SortState<TKey> {
  orderBy: TKey;
  direction: SortOrder;
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
