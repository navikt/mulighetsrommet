import { useAtom } from "jotai";
import { useTitle } from "mulighetsrommet-frontend-common";
import { useEffect } from "react";
import { avtaleFilterForTiltakstypeAtom } from "../../../api/atoms";
import { Avtalefilter } from "../../../components/filter/Avtalefilter";
import { AvtaleTabell } from "../../../components/tabell/AvtaleTabell";
import { useGetTiltakstypeIdFromUrl } from "../../../hooks/useGetTiltakstypeIdFromUrl";
import { ContainerLayout } from "../../../layouts/ContainerLayout";
import { useAvtaler } from "../../../api/avtaler/useAvtaler";

export function AvtalerForTiltakstype() {
  useTitle("Tiltakstyper - Avtaler");
  const tiltakstypeId = useGetTiltakstypeIdFromUrl();
  const [filter, setFilter] = useAtom(avtaleFilterForTiltakstypeAtom);
  const { data: avtaler } = useAvtaler(avtaleFilterForTiltakstypeAtom);

  useEffect(() => {
    if (tiltakstypeId) {
      // For å filtrere på avtaler for den spesifikke tiltakstypen
      setFilter({
        ...filter,
        tiltakstype: tiltakstypeId,
      });
    }
  }, [tiltakstypeId]);

  if (!avtaler) {
    return null;
  }

  return (
    <ContainerLayout>
      <Avtalefilter
        filterAtom={avtaleFilterForTiltakstypeAtom}
        skjulFilter={{
          tiltakstype: true,
        }}
      />
      <AvtaleTabell paginerteAvtaler={avtaler} avtalefilter={avtaleFilterForTiltakstypeAtom} />
    </ContainerLayout>
  );
}
