import { useQuery } from "@tanstack/react-query";
import { useAtom, WritableAtom } from "jotai";
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
    tiltakstypeIder: filter.tiltakstype ? [filter.tiltakstype] : [],
    search: debouncedSok || undefined,
    statuser: filter.status ? [filter.status] : [],
    navRegioner: filter.navRegion ? [filter.navRegion] : [],
    sort: filter.sortering,
    page,
    size: filter.antallAvtalerVises,
    leverandorOrgnr: filter.leverandor_orgnr ? [filter.leverandor_orgnr] : [],
  };

  return useQuery({
    queryKey: QueryKeys.avtaler(filter.visMineAvtaler, page, { ...filter, sok: debouncedSok }),

    queryFn: () => {
      return filter.visMineAvtaler
        ? mulighetsrommetClient.avtaler.getMineAvtaler({ ...queryFilter })
        : mulighetsrommetClient.avtaler.getAvtaler({ ...queryFilter });
    },
  });
}
