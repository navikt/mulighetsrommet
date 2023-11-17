import { useAtom } from "jotai";
import { useEffect } from "react";
import { avtaleFilter } from "../../../api/atoms";
import { useAvtaler } from "../../../api/avtaler/useAvtaler";
import { Avtalefilter } from "../../../components/filter/Avtalefilter";
import { useGetTiltakstypeIdFromUrl } from "../../../hooks/useGetTiltakstypeIdFromUrl";
import { AvtaleTabell } from "../../../components/tabell/AvtaleTabell";
import { ContainerLayout } from "../../../layouts/ContainerLayout";
import { useTitle } from "mulighetsrommet-frontend-common";

export function AvtalerForTiltakstype() {
  useTitle("Tiltakstyper - Avtaler");
  const tiltakstypeId = useGetTiltakstypeIdFromUrl();
  const { data } = useAvtaler();
  const [filter, setFilter] = useAtom(avtaleFilter);

  useEffect(() => {
    if (tiltakstypeId) {
      // For å filtrere på avtaler for den spesifikke tiltakstypen
      setFilter({
        ...filter,
        tiltakstype: tiltakstypeId,
      });
    }
  }, [tiltakstypeId]);

  if (!data) {
    return null;
  }

  return (
    <ContainerLayout>
      <Avtalefilter
        filter={filter}
        setFilter={setFilter}
        skjulFilter={{
          tiltakstype: true,
        }}
      />
      <AvtaleTabell />
    </ContainerLayout>
  );
}
