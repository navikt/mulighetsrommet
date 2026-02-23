import { useAtom, WritableAtom } from "jotai";
import { useCallback, useEffect, useMemo } from "react";
import { dequal } from "dequal";
import { FilterAction, FilterState } from "@/filter/filter-state";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";
import { LagretFilterType } from "@tiltaksadministrasjon/api-client";

export function useSavedFiltersState<T extends object>(
  filterStateAtom: WritableAtom<FilterState<T>, [FilterAction<T>], void>,
  type: LagretFilterType,
  parser: (input: unknown) => T,
) {
  const { filters, saveFilter, deleteFilter, setDefaultFilter } = useLagredeFilter(type);

  const [{ filter, defaultFilter }, dispatch] = useAtom(filterStateAtom);

  const savedDefaultFilter = useMemo(() => {
    const defaultFilter = filters.find((f) => f.isDefault);
    return defaultFilter
      ? { id: defaultFilter.id, values: parser(defaultFilter.filter) }
      : undefined;
  }, [filters, parser]);

  useEffect(() => {
    dispatch({ type: "UPDATE_DEFAULT", payload: savedDefaultFilter });
  }, [savedDefaultFilter, dispatch]);

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
            values: parser(savedFilter.filter),
          },
        });
      }
    },
    [filters, dispatch, parser],
  );

  const hasChanged = useMemo(
    () => !dequal(filter.values, defaultFilter.values),
    [filter.values, defaultFilter.values],
  );

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
