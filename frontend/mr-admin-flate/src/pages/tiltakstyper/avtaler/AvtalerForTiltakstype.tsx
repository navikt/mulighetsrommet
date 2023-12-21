import { useAtom } from "jotai";
import { useTitle } from "mulighetsrommet-frontend-common";
import { avtaleFilterForTiltakstypeAtom } from "../../../api/atoms";
import { AvtaleTabell } from "../../../components/tabell/AvtaleTabell";
import { useGetTiltakstypeIdFromUrlOrThrow } from "../../../hooks/useGetTiltakstypeIdFromUrl";
import { ContainerLayout } from "../../../layouts/ContainerLayout";
import { FilterAndTableLayout } from "../../../components/filter/FilterAndTableLayout";
import { AvtaleFilterTags } from "../../../components/filter/AvtaleFilterTags";
import { AvtaleFilterButtons } from "../../../components/filter/AvtaleFilterButtons";
import { AvtaleFilter } from "../../../components/filter/Avtalefilter";
import { useEffect } from "react";

export function AvtalerForTiltakstype() {
  useTitle("Tiltakstyper - Avtaler");

  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();

  const [filter, setFilter] = useAtom(avtaleFilterForTiltakstypeAtom);

  useEffect(() => {
    setFilter({ ...filter, page: 1, tiltakstyper: [tiltakstypeId] });
  }, [tiltakstypeId]);

  return (
    <ContainerLayout>
      <FilterAndTableLayout
        filter={
          <AvtaleFilter
            filterAtom={avtaleFilterForTiltakstypeAtom}
            skjulFilter={{
              tiltakstype: true,
            }}
          />
        }
        tags={<AvtaleFilterTags filterAtom={avtaleFilterForTiltakstypeAtom} />}
        buttons={<AvtaleFilterButtons filterAtom={avtaleFilterForTiltakstypeAtom} />}
        table={<AvtaleTabell filterAtom={avtaleFilterForTiltakstypeAtom} />}
      />
    </ContainerLayout>
  );
}
