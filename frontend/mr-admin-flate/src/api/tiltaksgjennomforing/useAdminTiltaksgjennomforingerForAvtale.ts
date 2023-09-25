import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { useAtom } from "jotai";
import { paginationAtom, tiltaksgjennomforingTilAvtaleFilter } from "../atoms";
import { useDebounce } from "mulighetsrommet-frontend-common";

export function useAdminTiltaksgjennomforingerForAvtale() {
  const [page] = useAtom(paginationAtom);
  const [filter] = useAtom(tiltaksgjennomforingTilAvtaleFilter);
  const search = useDebounce(filter.search, 300);

  return useQuery(
    QueryKeys.tiltaksgjennomforingerTilAvtale(search),
    () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforinger({
        page,
        search: search || undefined,
        size: 1000,
      }),
    { enabled: !!search },
  );
}
