import { useAtom } from "jotai";
import { useTitle } from "mulighetsrommet-frontend-common";
import { avtaleFilterForTiltakstypeAtom, avtalePaginationAtom } from "../../../api/atoms";
import { AvtaleTabell } from "../../../components/tabell/AvtaleTabell";
import { useGetTiltakstypeIdFromUrlOrThrow } from "../../../hooks/useGetTiltakstypeIdFromUrl";
import { ContainerLayout } from "../../../layouts/ContainerLayout";
import { useAvtaler } from "../../../api/avtaler/useAvtaler";
import { FilterAndTableLayout } from "../../../components/filter/FilterAndTableLayout";
import { AvtaleFilterTags } from "../../../components/filter/AvtaleFilterTags";
import { AvtaleFilterButtons } from "../../../components/filter/AvtaleFilterButtons";
import { AvtaleFilter } from "../../../components/filter/Avtalefilter";

export function AvtalerForTiltakstype() {
  useTitle("Tiltakstyper - Avtaler");

  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();

  const [page] = useAtom(avtalePaginationAtom);
  const { data: avtaler, isLoading: avtalerIsLoading } = useAvtaler(
    { tiltakstyper: [tiltakstypeId] },
    page,
  );

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
