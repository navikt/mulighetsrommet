import { useTitle } from "mulighetsrommet-frontend-common";
import { getAvtalerForTiltakstypeFilterAtom } from "@/api/atoms";
import { AvtaleTabell } from "../../../components/tabell/AvtaleTabell";
import { useGetTiltakstypeIdFromUrlOrThrow } from "../../../hooks/useGetTiltakstypeIdFromUrl";
import { ContainerLayout } from "../../../layouts/ContainerLayout";
import { FilterAndTableLayout } from "../../../components/filter/FilterAndTableLayout";
import { AvtaleFiltertags } from "../../../components/filter/AvtaleFiltertags";
import { AvtaleFilterButtons } from "../../../components/filter/AvtaleFilterButtons";
import { AvtaleFilter } from "../../../components/filter/AvtaleFilter";
import { useState } from "react";

export function AvtalerForTiltakstype() {
  useTitle("Tiltakstyper - Avtaler");

  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();

  const filterAtom = getAvtalerForTiltakstypeFilterAtom(tiltakstypeId);
  const [filterOpen, setFilterOpen] = useState<boolean>(true);

  return (
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
          />
        }
        buttons={<AvtaleFilterButtons filterAtom={filterAtom} tiltakstypeId={tiltakstypeId} />}
        table={<AvtaleTabell filterAtom={filterAtom} />}
        filterOpen={filterOpen}
        setFilterOpen={setFilterOpen}
      />
    </ContainerLayout>
  );
}
