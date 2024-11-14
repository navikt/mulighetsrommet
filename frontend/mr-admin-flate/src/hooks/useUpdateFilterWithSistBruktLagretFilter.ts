import { LagretDokumenttype } from "@mr/api-client";
import { useGetLagredeFilterForDokumenttype } from "@mr/frontend-common/components/lagreFilter/useGetLagredeFilterForDokumenttype";
import { useAtom, WritableAtom } from "jotai";
import { useEffect } from "react";
import { AvtaleFilter, TiltaksgjennomforingFilter } from "../api/atoms";

export function useUpdateFilterWithSistBruktLagretFilter(
  dokumenttype: LagretDokumenttype,
  filterAtom: WritableAtom<AvtaleFilter | TiltaksgjennomforingFilter, any, void>,
) {
  const { data: lagredeFilter = [] } = useGetLagredeFilterForDokumenttype(dokumenttype);
  const [filter, setFilter] = useAtom(filterAtom);
  const sistBrukteFilter = [...lagredeFilter]
    .filter((filter) => filter.sistBrukt)
    .sort((a, b) => {
      if (a.sistBrukt && b.sistBrukt) {
        return new Date(b.sistBrukt).getTime() - new Date(a.sistBrukt).getTime();
      }

      return 0;
    })
    .at(0);

  useEffect(() => {
    if (sistBrukteFilter) {
      setFilter({
        ...(sistBrukteFilter.filter as any),
        lagretFilterIdValgt: sistBrukteFilter.id,
      });
    }
  }, [filter, setFilter, sistBrukteFilter]);
}
