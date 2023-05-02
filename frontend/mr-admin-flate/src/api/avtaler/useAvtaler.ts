import { useQuery } from "@tanstack/react-query";
import { useAtom } from "jotai";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { AVTALE_PAGE_SIZE } from "../../constants";
import { avtaleFilter, avtalePaginationAtom } from "../atoms";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useAvtaler() {
  const [page] = useAtom(avtalePaginationAtom);
  const [filter] = useAtom(avtaleFilter);
  const debouncedSok = useDebounce(filter.sok, 300);

  return useQuery(
    QueryKeys.avtaler(
      filter.tiltakstype,
      debouncedSok,
      filter.status,
      filter.fylkeenhet,
      filter.sortering,
      page
    ),
    () => {
      return mulighetsrommetClient.avtaler.getAvtaler({
        tiltakstypeId: filter.tiltakstype || undefined,
        search: debouncedSok || undefined,
        avtalestatus: filter.status ? filter.status : undefined,
        fylkesenhet: filter.fylkeenhet ? filter.fylkeenhet : undefined,
        sort: filter.sortering,
        page,
        size: AVTALE_PAGE_SIZE,
      });
    }
  );
}
