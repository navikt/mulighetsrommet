import { ArrangorIkon } from "@/components/ikoner/ArrangorIkon";
import { ArrangorerTabell } from "@/components/tabell/ArrangorerTabell";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { arrangorerFilterStateAtom } from "@/pages/arrangor/filter";
import { useFilterState } from "@/filter/useFilterState";
import { Box, Chips, TextField } from "@navikt/ds-react";

export function ArrangorerPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);

  const { filter, updateFilter, resetToDefault, hasChanged } =
    useFilterState(arrangorerFilterStateAtom);

  const tag = filter.values.sok && (
    <Chips.Removable
      onClick={() => {
        updateFilter({ sok: "" });
      }}
    >
      {filter.values.sok}
    </Chips.Removable>
  );

  return (
    <>
      <title>Arrangører</title>
      <HeaderBanner heading="Arrangører" ikon={<ArrangorIkon />} />
      <ReloadAppErrorBoundary>
        <ContentBox>
          <FilterAndTableLayout
            filter={
              <Box margin="space-8">
                <TextField
                  label="Søk etter arrangør"
                  hideLabel
                  size="small"
                  placeholder="Søk etter arrangør"
                  onChange={(e) => {
                    updateFilter({
                      sok: e.currentTarget.value,
                      page: 1,
                    });
                  }}
                  value={filter.values.sok}
                  aria-label="Søk etter arrangør"
                />
              </Box>
            }
            nullstillFilterButton={
              hasChanged ? <NullstillFilterKnapp onClick={resetToDefault} /> : null
            }
            tags={tag}
            buttons={null}
            table={
              <ArrangorerTabell
                filter={filter.values}
                updateFilter={updateFilter}
                filterOpen={filterOpen}
              />
            }
            setFilterOpen={setFilterOpen}
            filterOpen={filterOpen}
          />
        </ContentBox>
      </ReloadAppErrorBoundary>
    </>
  );
}
