import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { paginationAtom } from "../atoms";

export function useTiltaksgjennomforinger() {
  const [page] = useAtom(paginationAtom);
  return useQuery(QueryKeys.tiltaksgjennomforinger(page), () =>
    mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforinger({
      page,
      size: PAGE_SIZE,
    })
  );
}
