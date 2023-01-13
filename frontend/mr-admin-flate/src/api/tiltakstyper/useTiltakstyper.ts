import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { paginationAtom, tiltakstypefilter } from "../atoms";

export function useTiltakstyper() {
  const [page] = useAtom(paginationAtom);
  const [sokefilter] = useAtom(tiltakstypefilter);
  return useQuery(QueryKeys.tiltakstyper(sokefilter, page), () =>
    mulighetsrommetClient.tiltakstyper.getTiltakstyper({
      search: sokefilter !== "" ? sokefilter : undefined,
      page,
      size: PAGE_SIZE,
    })
  );
}
