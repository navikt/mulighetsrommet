import { useTitle } from "mulighetsrommet-frontend-common";
import { getAvtalerForTiltakstypeFilterAtom } from "../../../api/atoms";
import { AvtaleTabell } from "../../../components/tabell/AvtaleTabell";
import { useGetTiltakstypeIdFromUrlOrThrow } from "../../../hooks/useGetTiltakstypeIdFromUrl";
import { ContainerLayout } from "../../../layouts/ContainerLayout";
import { FilterAndTableLayout } from "../../../components/filter/FilterAndTableLayout";
import { AvtaleFilterTags } from "../../../components/filter/AvtaleFilterTags";
import { AvtaleFilterButtons } from "../../../components/filter/AvtaleFilterButtons";
import { AvtaleFilter } from "../../../components/filter/Avtalefilter";

export function AvtalerForTiltakstype() {
  useTitle("Tiltakstyper - Avtaler");

  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();

  const filterAtom = getAvtalerForTiltakstypeFilterAtom(tiltakstypeId);

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
        tags={<AvtaleFilterTags filterAtom={filterAtom} tiltakstypeId={tiltakstypeId} />}
        buttons={<AvtaleFilterButtons filterAtom={filterAtom} tiltakstypeId={tiltakstypeId} />}
        table={<AvtaleTabell filterAtom={filterAtom} />}
      />
    </ContainerLayout>
  );
}
