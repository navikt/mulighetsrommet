import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { paginationAtom, tiltakstypefilter } from "../atoms";
import { Tiltakstypestatus } from "mulighetsrommet-api-client";

export function useTiltakstyper() {
  const [page] = useAtom(paginationAtom);
  const [sokefilter] = useAtom(tiltakstypefilter);
  return useQuery(
    QueryKeys.tiltakstyper(
      sokefilter.sok,
      sokefilter.status,
      sokefilter.tags,
      sokefilter.kategori,
      page
    ),
    () =>
      mulighetsrommetClient.tiltakstyper.getTiltakstyper({
        search: sokefilter.sok !== "" ? sokefilter.sok : undefined,
        tiltakstypestatus: sokefilter.status ?? Tiltakstypestatus.AKTIV,
        tiltakstypekategori: sokefilter.kategori ?? undefined,
        tags: sokefilter.tags.join(","),
        page,
        size: PAGE_SIZE,
      })
  );
}
