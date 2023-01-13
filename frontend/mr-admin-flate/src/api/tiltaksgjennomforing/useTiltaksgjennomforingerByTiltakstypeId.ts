import { useQuery } from "@tanstack/react-query";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { paginationAtomTiltaksgjennomforingMedTiltakstype } from "../atoms";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useTiltaksgjennomforingerByTiltakstypeId(id: string) {
  const [page] = useAtom(paginationAtomTiltaksgjennomforingMedTiltakstype);
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
