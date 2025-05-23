import { avtaleFilterAtom, AvtaleFilterSchema, AvtaleFilterType } from "@/api/atoms";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";
import { useSlettFilter } from "@/api/lagret-filter/useSlettFilter";
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
import { LagredeFilterOversikt, useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useAtom } from "jotai/index";
import { useState } from "react";
import { useLagreFilter } from "@/api/lagret-filter/useLagreFilter";

export function AvtalerPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);
  const [filter, setFilter] = useAtom(avtaleFilterAtom);
  const { data: lagredeFilter = [] } = useLagredeFilter(LagretFilterType.AVTALE);
  const deleteFilterMutation = useSlettFilter();
  const lagreFilterMutation = useLagreFilter();

  return (
    <main>
      <title>Avtaler</title>
      <HeaderBanner heading="Oversikt over avtaler" harUndermeny ikon={<AvtaleIkon />} />
      <ReloadAppErrorBoundary>
        <ContentBox>
          <FilterAndTableLayout
            nullstillFilterButton={<NullstillKnappForAvtaler filterAtom={avtaleFilterAtom} />}
            filter={<AvtaleFilter filterAtom={avtaleFilterAtom} />}
            lagredeFilter={
              <LagredeFilterOversikt
                filter={filter}
                lagredeFilter={lagredeFilter}
                onSetFilter={(filter) => {
                  setFilter(filter as AvtaleFilterType);
                }}
                onDeleteFilter={(id) => {
                  deleteFilterMutation.mutate(id);
                }}
                onSetDefaultFilter={(id, isDefault) => {
                  const filter = lagredeFilter.find((f) => f.id === id);
                  if (filter) {
                    lagreFilterMutation.mutate({ ...filter, isDefault });
                  }
                }}
                validateFilterStructure={(filter) => {
                  return AvtaleFilterSchema.safeParse(filter).success;
                }}
              />
            }
            tags={
              <AvtaleFilterTags
                filterAtom={avtaleFilterAtom}
                filterOpen={filterOpen}
                setTagsHeight={setTagsHeight}
              />
            }
            buttons={<AvtaleFilterButtons />}
            table={
              <AvtaleTabell
                filterAtom={avtaleFilterAtom}
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
