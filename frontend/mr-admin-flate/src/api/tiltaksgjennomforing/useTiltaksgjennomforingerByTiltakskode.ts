import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { paginationAtom } from "../atoms";

export function useTiltaksgjennomforingerByTiltakskode(tiltakskode: string) {
  const [page] = useAtom(paginationAtom);
  return useQuery(
    QueryKeys.tiltaksgjennomforingerByTiltakskode(tiltakskode, page),
    () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getAllByTiltakskode({
        tiltakskode,
        page,
        size: PAGE_SIZE,
      })
  );
}
