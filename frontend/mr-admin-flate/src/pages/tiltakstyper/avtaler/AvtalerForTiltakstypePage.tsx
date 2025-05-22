import { getAvtalerForTiltakstypeFilterAtom } from "@/api/atoms";
import { AvtaleFilter } from "@/components/filter/AvtaleFilter";
import { AvtaleFilterButtons } from "@/components/filter/AvtaleFilterButtons";
import { AvtaleFilterTags } from "@/components/filter/AvtaleFilterTags";
import { AvtaleTabell } from "@/components/tabell/AvtaleTabell";
import { useGetTiltakstypeIdFromUrlOrThrow } from "@/hooks/useGetTiltakstypeIdFromUrl";
import { ContentBox } from "@/layouts/ContentBox";
import { NullstillKnappForAvtaler } from "@/pages/avtaler/NullstillKnappForAvtaler";
import { useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useState } from "react";

export function AvtalerForTiltakstypePage() {
  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();
  const filterAtom = getAvtalerForTiltakstypeFilterAtom(tiltakstypeId);
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);

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
          tags={
            <AvtaleFilterTags
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
