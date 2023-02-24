import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { paginationAtom, tiltakstypefilter } from "../atoms";
import { useDebounce } from "mulighetsrommet-frontend-common";

export function useTiltakstyper() {
  const [page] = useAtom(paginationAtom);
  const [sokefilter] = useAtom(tiltakstypefilter);
  const debouncedSok = useDebounce(sokefilter.sok, 300);

  return useQuery(
    QueryKeys.tiltakstyper(
      debouncedSok,
      sokefilter.status,
      sokefilter.kategori,
      sokefilter.sortering,
      page
    ),
    () =>
      mulighetsrommetClient.tiltakstyper.getTiltakstyper({
        search: debouncedSok !== "" ? debouncedSok : undefined,
        tiltakstypestatus: sokefilter.status ?? undefined,
        tiltakstypekategori: sokefilter.kategori ?? undefined,
        sort: sokefilter.sortering ?? undefined,
        page,
        size: PAGE_SIZE,
      })
  );
}
