import { useAtom, WritableAtom } from "jotai";
import { useCallback, useEffect } from "react";
import { dequal } from "dequal";
import { FilterAction, FilterState } from "@/filter/filter-state";

export function useFilterState<T extends object>(
  filterStateAtom: WritableAtom<FilterState<T>, [FilterAction<T>], void>,
  lagredeFilter: Array<{ id: string; isDefault: boolean; filter: unknown }> = [],
) {
  const [{ filter, defaultFilter }, dispatch] = useAtom(filterStateAtom);

  useEffect(() => {
    const defaultFilter = lagredeFilter.find((f) => f.isDefault);
    const newDefault = defaultFilter
      ? { id: defaultFilter.id, values: defaultFilter.filter as T }
      : undefined;
    dispatch({ type: "UPDATE_DEFAULT", payload: newDefault });
  }, [lagredeFilter, dispatch]);

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

  const selectFilter = useCallback(
    (filterId: string) => {
      const savedFilter = lagredeFilter.find((f) => f.id === filterId);
      if (savedFilter) {
        dispatch({
          type: "SELECT_FILTER",
          payload: {
            id: filterId,
            values: savedFilter.filter as T,
          },
        });
      }
    },
    [lagredeFilter, dispatch],
  );

  const hasChanged = !dequal(filter.values, defaultFilter.values);

  return {
    filter,
    setFilter,
    updateFilter,
    resetToDefault,
    selectFilter,
    hasChanged,
  };
}
