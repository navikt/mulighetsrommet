import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { paginationAtom } from "../atoms";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";

export function useTiltakstyper() {
  const [page] = useAtom(paginationAtom);
  return useQuery(QueryKeys.tiltakstyper(page), () =>
    mulighetsrommetClient.tiltakstyper.getTiltakstyper({
      page,
      size: PAGE_SIZE,
    })
  );
}
