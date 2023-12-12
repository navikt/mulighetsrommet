import { useAtom } from "jotai";
import { useTitle } from "mulighetsrommet-frontend-common";
import { useEffect } from "react";
import { avtaleFilterForTiltakstypeAtom } from "../../../api/atoms";
import { AvtaleTabell } from "../../../components/tabell/AvtaleTabell";
import { useGetTiltakstypeIdFromUrl } from "../../../hooks/useGetTiltakstypeIdFromUrl";
import { ContainerLayout } from "../../../layouts/ContainerLayout";
import { useAvtaler } from "../../../api/avtaler/useAvtaler";
import { FilterAndTableLayout } from "../../../components/filter/FilterAndTableLayout";
import { AvtaleFilterTags } from "../../../components/filter/AvtaleFilterTags";
import { AvtaleFilterButtons } from "../../../components/filter/AvtaleFilterButtons";
import { AvtaleFilter } from "../../../components/filter/Avtalefilter";

export function AvtalerForTiltakstype() {
  useTitle("Tiltakstyper - Avtaler");
  const tiltakstypeId = useGetTiltakstypeIdFromUrl();
  const [filter, setFilter] = useAtom(avtaleFilterForTiltakstypeAtom);
  const { data: avtaler, isLoading: avtalerIsLoading } = useAvtaler(avtaleFilterForTiltakstypeAtom);

  useEffect(() => {
    if (tiltakstypeId) {
      // For å filtrere på avtaler for den spesifikke tiltakstypen
      setFilter({
        ...filter,
        tiltakstyper: [tiltakstypeId],
      });
    }
  }, [tiltakstypeId]);

  if (!avtaler) {
    return null;
  }

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
        table={
          <AvtaleTabell
            isLoading={avtalerIsLoading}
            paginerteAvtaler={avtaler}
            avtalefilter={avtaleFilterForTiltakstypeAtom}
          />
        }
      />
    </ContainerLayout>
  );
}
