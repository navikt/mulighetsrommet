import { useQuery } from "@tanstack/react-query";
import { WritableAtom, useAtom } from "jotai";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { QueryKeys } from "../QueryKeys";
import { AvtaleFilterProps, avtalePaginationAtomAtom } from "../atoms";
import { mulighetsrommetClient } from "../clients";

export function useAvtaler(
  filterAtom: WritableAtom<AvtaleFilterProps, [newValue: AvtaleFilterProps], void>,
) {
  const [page] = useAtom(avtalePaginationAtomAtom);
  const [filter] = useAtom(filterAtom);
  const debouncedSok = useDebounce(filter.sok, 300);

  const queryFilter = {
    tiltakstypeId: filter.tiltakstype || undefined,
    search: debouncedSok || undefined,
    avtalestatus: filter.status ? filter.status : undefined,
    navRegion: filter.navRegion ? filter.navRegion : undefined,
    sort: filter.sortering,
    page,
    size: filter.antallAvtalerVises,
    leverandorOrgnr: filter.leverandor_orgnr || undefined,
  };

  return useQuery({
    queryKey: QueryKeys.avtaler({ ...filter, sok: debouncedSok }, page),

    queryFn: () => {
      return filter.visMineAvtaler
        ? mulighetsrommetClient.avtaler.getMineAvtaler({ ...queryFilter })
        : mulighetsrommetClient.avtaler.getAvtaler({ ...queryFilter });
    },
  });
}
