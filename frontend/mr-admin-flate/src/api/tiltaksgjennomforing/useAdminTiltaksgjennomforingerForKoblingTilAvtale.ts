import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { useAtom } from "jotai";
import { paginationAtom, tiltaksgjennomforingTilAvtaleFilterAtom } from "../atoms";
import { useDebounce } from "mulighetsrommet-frontend-common";

export function useAdminTiltaksgjennomforingerForKoblingTilAvtale() {
  const [page] = useAtom(paginationAtom);
  const [filter] = useAtom(tiltaksgjennomforingTilAvtaleFilterAtom);
  const search = useDebounce(filter.search, 300);

  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforingerTilAvtale(search),
    queryFn: () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforinger({
        page,
        search: search || undefined,
        size: 1000,
      }),
    enabled: !!search,
  });
}
