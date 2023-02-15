import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { paginationAtom, tiltakstypefilter } from "../atoms";
import { Tiltakstypestatus } from "mulighetsrommet-api-client";
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
      page
    ),
    () =>
      mulighetsrommetClient.tiltakstyper.getTiltakstyper({
        search: debouncedSok !== "" ? debouncedSok : undefined,
        tiltakstypestatus: sokefilter.status ?? Tiltakstypestatus.AKTIV,
        tiltakstypekategori: sokefilter.kategori ?? undefined,
        page,
        size: PAGE_SIZE,
      })
  );
}
