import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import {
  LagredeFilterOversikt,
  LagreFilterButton,
  ListSkeleton,
  useOpenFilterWhenThreshold,
} from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { CurrencyExchangeIcon } from "@navikt/aksel-icons";
import { Suspense } from "react";
import { useSavedFiltersState } from "@/filter/useSavedFiltersState";
import { LagretFilterType } from "@tiltaksadministrasjon/api-client";
import { InnsendingFilterSchema, InnsendingFilterStateAtom } from "./filter";
import { InnsendingFilter } from "./Innsendingfilter";
import { InnsendingTable } from "./InnsendingTable";
import { InnsendingFilterTags } from "./InnsendingFilterTags";

export function InnsendingoversiktPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const {
    filter,
    resetFilterToDefault,
    selectFilter,
    hasChanged,
    updateFilter,
    filters,
    saveFilter,
    deleteFilter,
    setDefaultFilter,
  } = useSavedFiltersState(InnsendingFilterStateAtom, LagretFilterType.INNSENDING);

  return (
    <>
      <title>Oversikt over manglende innsendinger</title>
      <HeaderBanner
        heading="Oversikt over manglende innsendinger"
        ikon={<CurrencyExchangeIcon width="40" height="40" color="#3380A5" />}
      />
      <ContentBox>
        <FilterAndTableLayout
          filter={<InnsendingFilter filter={filter.values} updateFilter={updateFilter} />}
          nullstillFilterButton={
            hasChanged ? (
              <>
                <NullstillFilterKnapp onClick={resetFilterToDefault} />
                <LagreFilterButton filter={filter.values} onLagre={saveFilter} />
              </>
            ) : null
          }
          lagredeFilter={
            <LagredeFilterOversikt
              filters={filters}
              selectedFilterId={filter.id}
              onSelectFilterId={selectFilter}
              onDeleteFilter={deleteFilter}
              onSetDefaultFilter={setDefaultFilter}
              validateFilterStructure={(filter) => {
                return InnsendingFilterSchema.safeParse(filter).success;
              }}
            />
          }
          tags={
            <InnsendingFilterTags
              filter={filter.values}
              updateFilter={updateFilter}
              filterOpen={filterOpen}
            />
          }
          buttons={null}
          table={
            <Suspense fallback={<ListSkeleton />}>
              <InnsendingTable updateFilter={updateFilter} />
            </Suspense>
          }
          filterOpen={filterOpen}
          setFilterOpen={setFilterOpen}
        />
      </ContentBox>
    </>
  );
}
