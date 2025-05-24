import {
  createFilterAtomWithAPI,
  createFilterValidator,
  defaultGjennomforingFilter,
  FilterAction,
  FilterManagerState,
  GjennomforingFilterSchema,
  GjennomforingFilterType,
} from "@/api/atoms";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";
import { GjennomforingFilter } from "@/components/filter/GjennomforingFilter";
import { GjennomforingFilterButtons } from "@/components/filter/GjennomforingFilterButtons";
import { GjennomforingFilterTags } from "@/components/filter/GjennomforingFilterTags";
import { GjennomforingTable } from "@/components/gjennomforing/GjennomforingTable";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { LagretFilterType } from "@mr/api-client-v2";
import {
  LagredeFilterOversikt,
  LagreFilterButton,
  useOpenFilterWhenThreshold,
} from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useAtom, WritableAtom } from "jotai";
import { useCallback, useEffect, useState } from "react";
import { dequal } from "dequal";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";

export const gjennomforingFilterManagerAtom = createFilterAtomWithAPI<GjennomforingFilterType>(
  "gjennomforing-filter",
  defaultGjennomforingFilter,
  createFilterValidator(GjennomforingFilterSchema),
);

export function useFilterWithInit<T extends object>(
  filterReducerAtom: WritableAtom<FilterManagerState<T>, [FilterAction<T>], void>,
  lagredeFilter: Array<{ id: string; isDefault: boolean; filter: unknown }>,
) {
  const [{ filter, defaultFilter }, dispatch] = useAtom(filterReducerAtom);

  useEffect(() => {
    const defaultFilter = lagredeFilter.find((f) => f.isDefault);
    if (defaultFilter) {
      dispatch({
        type: "UPDATE_DEFAULT",
        payload: {
          id: defaultFilter.id,
          values: defaultFilter.filter as T,
        },
      });
    }
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

export function GjennomforingerPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);

  const { lagredeFilter, lagreFilter, slettFilter, setDefaultFilter } = useLagredeFilter(
    LagretFilterType.GJENNOMFORING,
  );

  const { filter, setFilter, updateFilter, resetToDefault, selectFilter, hasChanged } =
    useFilterWithInit(gjennomforingFilterManagerAtom, lagredeFilter);

  return (
    <main>
      <title>Gjennomføringer</title>
      <HeaderBanner heading="Oversikt over gjennomføringer" ikon={<GjennomforingIkon />} />
      <ContentBox>
        <FilterAndTableLayout
          filter={<GjennomforingFilter filter={filter.values} updateFilter={updateFilter} />}
          nullstillFilterButton={
            <>
              {hasChanged ? <NullstillFilterKnapp onClick={resetToDefault} /> : null}
              <LagreFilterButton filter={filter.values} onLagre={lagreFilter} />
            </>
          }
          lagredeFilter={
            <LagredeFilterOversikt
              selectedFilterId={filter.id}
              onSelectFilterId={selectFilter}
              filter={filter.values}
              lagredeFilter={lagredeFilter}
              onSetFilter={(filter) => {
                setFilter(filter as GjennomforingFilterType);
              }}
              onDeleteFilter={slettFilter}
              onSetDefaultFilter={setDefaultFilter}
              validateFilterStructure={(filter) => {
                return GjennomforingFilterSchema.safeParse(filter).success;
              }}
            />
          }
          tags={
            <GjennomforingFilterTags
              filter={filter.values}
              updateFilter={updateFilter}
              filterOpen={filterOpen}
              setTagsHeight={setTagsHeight}
            />
          }
          buttons={<GjennomforingFilterButtons />}
          table={
            <ReloadAppErrorBoundary>
              <GjennomforingTable
                filter={filter.values}
                updateFilter={updateFilter}
                tagsHeight={tagsHeight}
                filterOpen={filterOpen}
              />
            </ReloadAppErrorBoundary>
          }
          filterOpen={filterOpen}
          setFilterOpen={setFilterOpen}
        />
      </ContentBox>
      <TilToppenKnapp />
    </main>
  );
}
