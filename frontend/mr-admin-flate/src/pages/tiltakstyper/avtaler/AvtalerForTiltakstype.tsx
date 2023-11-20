import { useAtom } from "jotai";
import { useTitle } from "mulighetsrommet-frontend-common";
import { useEffect } from "react";
import { avtaleFilterForTiltakstype } from "../../../api/atoms";
import { useAvtalerForTiltakstype } from "../../../api/avtaler/useAvtalerForTiltakstype";
import { Avtalefilter } from "../../../components/filter/Avtalefilter";
import { AvtaleTabell } from "../../../components/tabell/AvtaleTabell";
import { useGetTiltakstypeIdFromUrl } from "../../../hooks/useGetTiltakstypeIdFromUrl";
import { ContainerLayout } from "../../../layouts/ContainerLayout";

export function AvtalerForTiltakstype() {
  useTitle("Tiltakstyper - Avtaler");
  const tiltakstypeId = useGetTiltakstypeIdFromUrl();
  const { data: avtaler } = useAvtalerForTiltakstype();
  const [filter, setFilter] = useAtom(avtaleFilterForTiltakstype);

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
        avtalefilter={avtaleFilterForTiltakstype}
        skjulFilter={{
          tiltakstype: true,
        }}
      />
      <AvtaleTabell paginerteAvtaler={avtaler} avtalefilter={avtaleFilterForTiltakstype} />
    </ContainerLayout>
  );
}
