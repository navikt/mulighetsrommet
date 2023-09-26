import { useQuery } from "@tanstack/react-query";
import { useAtom } from "jotai";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { QueryKeys } from "../QueryKeys";
import { avtaleFilter, avtalePaginationAtom } from "../atoms";
import { mulighetsrommetClient } from "../clients";

export function useAvtaler() {
  const [page] = useAtom(avtalePaginationAtom);
  const [filter] = useAtom(avtaleFilter);
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

  return useQuery(QueryKeys.avtaler({ ...filter, sok: debouncedSok }, page), () => {
    return filter.visMineAvtaler
      ? mulighetsrommetClient.avtaler.getMineAvtaler({ ...queryFilter })
      : mulighetsrommetClient.avtaler.getAvtaler({ ...queryFilter });
  });
}
