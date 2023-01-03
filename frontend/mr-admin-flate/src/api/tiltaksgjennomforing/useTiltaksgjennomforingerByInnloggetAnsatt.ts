import { useQuery } from "@tanstack/react-query";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { paginationAtom } from "../atoms";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useTiltaksgjennomforingerByInnloggetAnsatt() {
  const [page] = useAtom(paginationAtom);

  return useQuery(QueryKeys.tiltaksgjennomforingerByAnsatt(page), () =>
    mulighetsrommetClient.tiltaksgjennomforinger.getAnsattsGjennomforinger({
      page,
      size: PAGE_SIZE,
    })
  );
}
