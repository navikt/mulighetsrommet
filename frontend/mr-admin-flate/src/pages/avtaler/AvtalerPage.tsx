import {
  avtaleFilterAtom,
  AvtaleFilterSchema,
  AvtaleFilterType,
  defaultAvtaleFilter,
} from "@/api/atoms";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";
import { AvtaleFilter } from "@/components/filter/AvtaleFilter";
import { AvtaleFilterButtons } from "@/components/filter/AvtaleFilterButtons";
import { AvtaleFilterTags } from "@/components/filter/AvtaleFilterTags";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { AvtaleTabell } from "@/components/tabell/AvtaleTabell";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { NullstillKnappForAvtaler } from "@/pages/avtaler/NullstillKnappForAvtaler";
import { LagretFilterType } from "@mr/api-client-v2";
import {
  LagredeFilterOversikt,
  LagreFilterButton,
  useOpenFilterWhenThreshold,
} from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useAtom } from "jotai/index";
import { useState } from "react";

export function AvtalerPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);
  const [filter, setFilter] = useAtom(avtaleFilterAtom);
  const { lagredeFilter, lagreFilter, slettFilter, setDefaultFilter } = useLagredeFilter(
    LagretFilterType.AVTALE,
  );

  function updateFilter(value: Partial<AvtaleFilterType>) {
    setFilter((prev) => ({ ...prev, ...value }));
  }

  function resetFilter() {
    setFilter(defaultAvtaleFilter);
  }

  return (
    <main>
      <title>Avtaler</title>
      <HeaderBanner heading="Oversikt over avtaler" harUndermeny ikon={<AvtaleIkon />} />
      <ReloadAppErrorBoundary>
        <ContentBox>
          <FilterAndTableLayout
            filter={<AvtaleFilter filter={filter} updateFilter={updateFilter} />}
            nullstillFilterButton={
              <>
                <NullstillKnappForAvtaler filter={filter} resetFilter={resetFilter} />
                <LagreFilterButton filter={filter} onLagre={lagreFilter} />
              </>
            }
            lagredeFilter={
              <LagredeFilterOversikt
                filter={filter}
                lagredeFilter={lagredeFilter}
                onSetFilter={(filter) => {
                  setFilter(filter as AvtaleFilterType);
                }}
                onDeleteFilter={slettFilter}
                onSetDefaultFilter={setDefaultFilter}
                validateFilterStructure={(filter) => {
                  return AvtaleFilterSchema.safeParse(filter).success;
                }}
              />
            }
            tags={
              <AvtaleFilterTags
                filter={filter}
                updateFilter={updateFilter}
                filterOpen={filterOpen}
                setTagsHeight={setTagsHeight}
              />
            }
            buttons={<AvtaleFilterButtons />}
            table={
              <AvtaleTabell
                filter={filter}
                updateFilter={updateFilter}
                tagsHeight={tagsHeight}
                filterOpen={filterOpen}
              />
            }
            setFilterOpen={setFilterOpen}
            filterOpen={filterOpen}
          />
        </ContentBox>
      </ReloadAppErrorBoundary>
      <TilToppenKnapp />
    </main>
  );
}
