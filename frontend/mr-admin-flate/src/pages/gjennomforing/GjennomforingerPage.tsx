import {
  gjennomforingfilterAtom,
  GjennomforingFilterSchema,
  GjennomforingFilterType,
} from "@/api/atoms";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";
import { useSlettFilter } from "@/api/lagret-filter/useSlettFilter";
import { GjennomforingFilter } from "@/components/filter/GjennomforingFilter";
import { GjennomforingFilterButtons } from "@/components/filter/GjennomforingFilterButtons";
import { GjennomforingFilterTags } from "@/components/filter/GjennomforingFilterTags";
import { GjennomforingTable } from "@/components/gjennomforing/GjennomforingTable";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { NullstillKnappForGjennomforinger } from "@/pages/gjennomforing/NullstillKnappForGjennomforinger";
import { LagretFilterType } from "@mr/api-client-v2";
import { LagredeFilterOversikt, useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useAtom } from "jotai/index";
import { useState } from "react";
import { useLagreFilter } from "@/api/lagret-filter/useLagreFilter";

export function GjennomforingerPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);
  const [filter, setFilter] = useAtom(gjennomforingfilterAtom);
  const { data: lagredeFilter = [] } = useLagredeFilter(LagretFilterType.GJENNOMFORING);
  const deleteFilterMutation = useSlettFilter();
  const lagreFilterMutation = useLagreFilter();

  return (
    <main>
      <title>Gjennomføringer</title>
      <HeaderBanner heading="Oversikt over gjennomføringer" ikon={<GjennomforingIkon />} />
      <ContentBox>
        <FilterAndTableLayout
          filter={<GjennomforingFilter filterAtom={gjennomforingfilterAtom} />}
          lagredeFilter={
            <LagredeFilterOversikt
              filter={filter}
              lagredeFilter={lagredeFilter}
              onSetFilter={(filter) => {
                setFilter(filter as GjennomforingFilterType);
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
                return GjennomforingFilterSchema.safeParse(filter).success;
              }}
            />
          }
          tags={
            <GjennomforingFilterTags
              filterAtom={gjennomforingfilterAtom}
              filterOpen={filterOpen}
              setTagsHeight={setTagsHeight}
            />
          }
          buttons={<GjennomforingFilterButtons />}
          table={
            <ReloadAppErrorBoundary>
              <GjennomforingTable
                filterAtom={gjennomforingfilterAtom}
                tagsHeight={tagsHeight}
                filterOpen={filterOpen}
              />
            </ReloadAppErrorBoundary>
          }
          filterOpen={filterOpen}
          setFilterOpen={setFilterOpen}
          nullstillFilterButton={
            <NullstillKnappForGjennomforinger filterAtom={gjennomforingfilterAtom} />
          }
        />
      </ContentBox>
      <TilToppenKnapp />
    </main>
  );
}
