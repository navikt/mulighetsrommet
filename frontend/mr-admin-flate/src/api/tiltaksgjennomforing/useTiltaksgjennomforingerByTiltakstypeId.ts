import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { paginationAtom } from "../atoms";

export function useTiltaksgjennomforingerByTiltakstypeId(id: string) {
  const [page] = useAtom(paginationAtom);
  return useQuery(
    QueryKeys.tiltaksgjennomforingerByTiltakstypeId(id, page),
    () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getAllByTiltakstype({
        id,
        page,
        size: PAGE_SIZE,
      })
  );
}
