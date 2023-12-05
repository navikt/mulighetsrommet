import { useQuery } from "@tanstack/react-query";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { PAGE_SIZE } from "../../constants";
import { TiltakstypeFilter } from "../atoms";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useTiltakstyper(filter: TiltakstypeFilter, page: number) {
  const debouncedSok = useDebounce(filter.sok || "", 300);
  return useQuery({
    queryKey: QueryKeys.tiltakstyper(debouncedSok, filter, page),

    queryFn: () =>
      mulighetsrommetClient.tiltakstyper.getTiltakstyper({
        search: debouncedSok !== "" ? debouncedSok : undefined,
        tiltakstypestatuser: filter.status ? [filter.status] : [],
        tiltakstypekategorier: filter.kategori ? [filter.kategori] : [],
        sort: filter.sortering,
        page,
        size: PAGE_SIZE,
      }),
  });
}
