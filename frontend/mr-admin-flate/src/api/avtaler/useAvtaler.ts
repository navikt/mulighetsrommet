import { useQuery } from "@tanstack/react-query";
import { useAtom, WritableAtom } from "jotai";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { QueryKeys } from "../QueryKeys";
import { AvtaleFilter, avtalePaginationAtom } from "../atoms";
import { mulighetsrommetClient } from "../clients";

export function useAvtaler(
  filterAtom: WritableAtom<AvtaleFilter, [newValue: AvtaleFilter], void>,
) {
  const [page] = useAtom(avtalePaginationAtom);
  const [filter] = useAtom(filterAtom);
  const debouncedSok = useDebounce(filter.sok, 300);

  const queryFilter = {
    tiltakstypeIder: filter.tiltakstyper,
    search: debouncedSok || undefined,
    statuser: filter.statuser,
    navRegioner: filter.navRegioner,
    sort: filter.sortering,
    page,
    size: filter.antallAvtalerVises,
    leverandorOrgnr: filter.leverandor_orgnr,
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
