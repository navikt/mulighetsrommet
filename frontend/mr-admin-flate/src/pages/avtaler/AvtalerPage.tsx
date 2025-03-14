import { avtaleFilterAtom, AvtaleFilterSchema } from "@/api/atoms";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";
import { useSlettFilter } from "@/api/lagret-filter/useSlettFilter";
import { AvtaleFilter } from "@/components/filter/AvtaleFilter";
import { AvtaleFilterButtons } from "@/components/filter/AvtaleFilterButtons";
import { AvtaleFiltertags } from "@/components/filter/AvtaleFiltertags";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { AvtaleTabell } from "@/components/tabell/AvtaleTabell";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { NullstillKnappForAvtaler } from "@/pages/avtaler/NullstillKnappForAvtaler";
import { LagretDokumenttype } from "@mr/api-client-v2";
import { LagredeFilterOversikt, useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useAtom } from "jotai/index";
import { useState } from "react";

export function AvtalerPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);
  const { data: lagredeFilter = [] } = useLagredeFilter(LagretDokumenttype.AVTALE);
  const deleteFilterMutation = useSlettFilter(LagretDokumenttype.AVTALE);

  const [filter, setFilter] = useAtom(avtaleFilterAtom);

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
                onDelete={(id: string) => deleteFilterMutation.mutate(id)}
                lagredeFilter={lagredeFilter}
                setFilter={setFilter}
                filter={filter}
                validateFilterStructure={(filter) => {
                  return AvtaleFilterSchema.safeParse(filter).success;
                }}
              />
            }
            tags={
              <AvtaleFiltertags
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
