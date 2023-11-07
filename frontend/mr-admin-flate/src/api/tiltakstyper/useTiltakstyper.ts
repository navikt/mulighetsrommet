import { useQuery } from "@tanstack/react-query";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { PAGE_SIZE } from "../../constants";
import { TiltakstypeFilter } from "../atoms";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useTiltakstyper(filter: TiltakstypeFilter, page: number) {
  const debouncedSok = useDebounce(filter.sok || "", 300);
  return useQuery({
    queryKey: QueryKeys.tiltakstyper(
      debouncedSok,
      filter.status,
      filter.kategori,
      filter.sortering,
      page,
    ),

    queryFn: () =>
      mulighetsrommetClient.tiltakstyper.getTiltakstyper({
        search: debouncedSok !== "" ? debouncedSok : undefined,
        tiltakstypestatus: filter.status || undefined,
        tiltakstypekategori: filter.kategori || undefined,
        sort: filter.sortering ?? undefined,
        page,
        size: PAGE_SIZE,
      }),
  });
}
