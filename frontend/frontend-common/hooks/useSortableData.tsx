import { useMemo, useState } from "react";
import { compare } from "../utils/utils";

export function useSortableData<T, V, K extends string = string>(
  data: T[],
  defaultSort?: { orderBy: K; direction: "ascending" | "descending" },
  getValue: (item: T, key: K) => V = (item, key) => (item as any)[key],
  comparator: (a: V, b: V) => number = compare,
) {
  const [sort, setSort] = useState(defaultSort);

  const sortedData = useMemo(() => {
    if (!sort) return data;

    const { orderBy, direction } = sort;

    return [...data].sort((a, b) => {
      const aVal = getValue(a, orderBy);
      const bVal = getValue(b, orderBy);

      return direction === "ascending" ? comparator(aVal, bVal) : comparator(bVal, aVal);
    });
  }, [data, sort, comparator, getValue]);

  const toggleSort = (key: K) => {
    setSort((prev) => {
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
