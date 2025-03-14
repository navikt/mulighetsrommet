import { AvtaleFilterSchema, getAvtalerForTiltakstypeFilterAtom } from "@/api/atoms";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";
import { useSlettFilter } from "@/api/lagret-filter/useSlettFilter";
import { AvtaleFilter } from "@/components/filter/AvtaleFilter";
import { AvtaleFilterButtons } from "@/components/filter/AvtaleFilterButtons";
import { AvtaleFiltertags } from "@/components/filter/AvtaleFiltertags";
import { AvtaleTabell } from "@/components/tabell/AvtaleTabell";
import { useGetTiltakstypeIdFromUrlOrThrow } from "@/hooks/useGetTiltakstypeIdFromUrl";
import { ContentBox } from "@/layouts/ContentBox";
import { NullstillKnappForAvtaler } from "@/pages/avtaler/NullstillKnappForAvtaler";
import { LagretDokumenttype } from "@mr/api-client-v2";
import { LagredeFilterOversikt, useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useAtom } from "jotai/index";
import { useState } from "react";

export function AvtalerForTiltakstypePage() {
  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();
  const filterAtom = getAvtalerForTiltakstypeFilterAtom(tiltakstypeId);
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);
  const [filter, setFilter] = useAtom(filterAtom);
  const { data: lagredeFilter = [] } = useLagredeFilter(LagretDokumenttype.AVTALE);
  const deleteFilterMutation = useSlettFilter(LagretDokumenttype.AVTALE);

  return (
    <>
      <ContentBox>
        <FilterAndTableLayout
          filter={
            <AvtaleFilter
              filterAtom={filterAtom}
              skjulFilter={{
                tiltakstype: true,
              }}
            />
          }
          lagredeFilter={
            <LagredeFilterOversikt
              setFilter={setFilter}
              lagredeFilter={lagredeFilter}
              onDelete={(id: string) => deleteFilterMutation.mutate(id)}
              filter={filter}
              validateFilterStructure={(filter) => {
                return AvtaleFilterSchema.safeParse(filter).success;
              }}
            />
          }
          tags={
            <AvtaleFiltertags
              filterAtom={filterAtom}
              tiltakstypeId={tiltakstypeId}
              filterOpen={filterOpen}
              setTagsHeight={setTagsHeight}
            />
          }
          buttons={<AvtaleFilterButtons />}
          table={
            <AvtaleTabell filterAtom={filterAtom} tagsHeight={tagsHeight} filterOpen={filterOpen} />
          }
          filterOpen={filterOpen}
          setFilterOpen={setFilterOpen}
          nullstillFilterButton={
            <NullstillKnappForAvtaler filterAtom={filterAtom} tiltakstypeId={tiltakstypeId} />
          }
        />
      </ContentBox>
      <TilToppenKnapp />
    </>
  );
}
