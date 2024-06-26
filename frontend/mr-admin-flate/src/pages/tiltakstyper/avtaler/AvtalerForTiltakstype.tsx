import { useTitle } from "mulighetsrommet-frontend-common";
import { getAvtalerForTiltakstypeFilterAtom } from "@/api/atoms";
import { AvtaleTabell } from "@/components/tabell/AvtaleTabell";
import { useGetTiltakstypeIdFromUrlOrThrow } from "@/hooks/useGetTiltakstypeIdFromUrl";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { FilterAndTableLayout } from "mulighetsrommet-frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { AvtaleFiltertags } from "@/components/filter/AvtaleFiltertags";
import { AvtaleFilterButtons } from "@/components/filter/AvtaleFilterButtons";
import { AvtaleFilter } from "@/components/filter/AvtaleFilter";
import { useState } from "react";
import { NullstillKnappForAvtaler } from "@/pages/avtaler/NullstillKnappForAvtaler";
import { TilToppenKnapp } from "mulighetsrommet-frontend-common/components/tilToppenKnapp/TilToppenKnapp";

export function AvtalerForTiltakstype() {
  useTitle("Tiltakstyper - Avtaler");

  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();
  const filterAtom = getAvtalerForTiltakstypeFilterAtom(tiltakstypeId);
  const [filterOpen, setFilterOpen] = useState<boolean>(true);
  const [tagsHeight, setTagsHeight] = useState(0);

  return (
    <>
      <ContainerLayout>
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
      </ContainerLayout>
      <TilToppenKnapp />
    </>
  );
}
