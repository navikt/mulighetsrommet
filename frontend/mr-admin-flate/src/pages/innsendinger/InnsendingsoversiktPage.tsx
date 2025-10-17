import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import {
  LagreFilterButton,
  LagredeFilterOversikt,
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
import { UtbetalingerForGjennomforingContainer } from "../gjennomforing/utbetaling/UtbetalingerForGjennomforingContainer";

export function InnsendingoversiktPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const {
    filter,
    resetFilterToDefault,
    selectFilter,
    hasChanged,
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
        harUndermeny
        ikon={<CurrencyExchangeIcon />}
      />
      <ContentBox>
        <FilterAndTableLayout
          filter={null}
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
          tags={null}
          buttons={null}
          table={
            <Suspense fallback={<ListSkeleton />}>
              <UtbetalingerForGjennomforingContainer />
            </Suspense>
          }
          filterOpen={filterOpen}
          setFilterOpen={setFilterOpen}
        />
      </ContentBox>
    </>
  );
}
