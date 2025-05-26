import { useAtom, WritableAtom } from "jotai";
import { useCallback, useEffect } from "react";
import { dequal } from "dequal";
import { FilterAction, FilterState } from "@/filter/filter-state";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";
import { LagretFilterType } from "@mr/api-client-v2";

export function useSavedFiltersState<T extends object>(
  filterStateAtom: WritableAtom<FilterState<T>, [FilterAction<T>], void>,
  type: LagretFilterType,
) {
  const { filters, saveFilter, deleteFilter, setDefaultFilter } = useLagredeFilter(type);

  const [{ filter, defaultFilter }, dispatch] = useAtom(filterStateAtom);

  useEffect(() => {
    const defaultFilter = filters.find((f) => f.isDefault);
    const newDefault = defaultFilter
      ? { id: defaultFilter.id, values: defaultFilter.filter as T }
      : undefined;
    dispatch({ type: "UPDATE_DEFAULT", payload: newDefault });
  }, [filters, dispatch]);

  const resetFilterToDefault = useCallback(() => {
    dispatch({ type: "RESET_TO_DEFAULT" });
  }, [dispatch]);

  const updateFilter = useCallback(
    (partialFilter: Partial<T>) => {
      dispatch({ type: "UPDATE_FILTER", payload: partialFilter });
    },
    [dispatch],
  );

  const selectFilter = useCallback(
    (filterId: string) => {
      const savedFilter = filters.find((f) => f.id === filterId);
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
    [filters, dispatch],
  );

  const hasChanged = !dequal(filter.values, defaultFilter.values);

  return {
    filter,
    updateFilter,
    resetFilterToDefault,
    selectFilter,
    hasChanged,
    filters,
    saveFilter,
    deleteFilter,
    setDefaultFilter,
  };
}
