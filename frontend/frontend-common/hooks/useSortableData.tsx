import { useMemo, useState } from 'react';
import { compare } from '../utils/utils';

export function useSortableData<T>(
  data: T[],
  defaultSort?: { orderBy: keyof T; direction: "ascending" | "descending" }
) {
  const [sort, setSort] = useState<typeof defaultSort | undefined>(defaultSort);

  const sortedData = useMemo(() => {
    if (!sort) {
      return data;
    }
    const sorted = [...data].sort((a, b) => {
      const aValue = a[sort.orderBy];
      const bValue = b[sort.orderBy];
      return sort.direction === "ascending"
        ? compare(bValue, aValue)
        : compare(aValue, bValue);
    });
    return sorted;
  }, [data, sort]);

  const toggleSort = (key: keyof T) => {
    setSort((prev) => {
      if (!prev || prev.orderBy !== key) {
        return { orderBy: key, direction: "ascending" };
      }
      if (prev.direction === "ascending") {
        return { orderBy: key, direction: "descending" };
      }
      return undefined; // clear sort
    });
  };

  return { sortedData, sort, setSort, toggleSort };
}
