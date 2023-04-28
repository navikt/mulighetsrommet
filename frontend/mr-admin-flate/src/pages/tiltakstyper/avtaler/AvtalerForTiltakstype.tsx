import { useAtom } from "jotai";
import { useEffect } from "react";
import { avtaleFilter } from "../../../api/atoms";
import { useAvtaler } from "../../../api/avtaler/useAvtaler";
import { Avtalefilter } from "../../../components/filter/Avtalefilter";
import { useGetTiltakstypeIdFromUrl } from "../../../hooks/useGetTiltakstypeIdFromUrl";
import { AvtaleTabell } from "../../../components/tabell/AvtaleTabell";

export function AvtalerForTiltakstype() {
  const tiltakstypeId = useGetTiltakstypeIdFromUrl();
  const { data } = useAvtaler();
  const [filter, setFilter] = useAtom(avtaleFilter);

  useEffect(() => {
    if (tiltakstypeId) {
      // For å filtrere på avtaler for den spesifikke tiltakstypen
      setFilter({ ...filter, tiltakstype: tiltakstypeId });
    }
  }, [tiltakstypeId]);

  if (!data) {
    return null;
  }

  return (
    <>
      <Avtalefilter skjulFilter={{ tiltakstype: true }} />
      <AvtaleTabell />
    </>
  );
}
