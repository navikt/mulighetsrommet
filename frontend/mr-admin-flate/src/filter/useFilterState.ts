import { useAtom, WritableAtom } from "jotai";
import { useCallback } from "react";
import { dequal } from "dequal";
import { FilterAction, FilterState } from "@/filter/filter-state";

export function useFilterState<T extends object>(
  filterStateAtom: WritableAtom<FilterState<T>, [FilterAction<T>], void>,
) {
  const [{
    
    
    filter, defaultFilter }, dispatch] = useAtom(filterStateAtom);

  const resetToDefault = useCallback(() => {
    dispatch({ type: "RESET_TO_DEFAULT" });
  }, [dispatch]);

  const setFilter = useCallback(
    (newFilter: T) => {
      dispatch({ type: "SET_FILTER", payload: newFilter });
    },
    [dispatch],
  );

  const updateFilter = useCallback(
    (partialFilter: Partial<T>) => {
      dispatch({ type: "UPDATE_FILTER", payload: partialFilter });
    },
    [dispatch],
  );

  const hasChanged = !dequal(filter.values, defaultFilter.values);

  return {
    filter,
    setFilter,
    updateFilter,
    resetToDefault,
    hasChanged,
  };
}
