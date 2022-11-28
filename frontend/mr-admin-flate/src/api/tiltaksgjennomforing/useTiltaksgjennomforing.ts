import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { useAtom } from "jotai";
import { paginationAtom } from "../atoms";

export function useTiltaksgjennomforing() {
  const [page] = useAtom(paginationAtom);
  return useQuery([QueryKeys.tiltaksgjennomforinger, page], () =>
    mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforinger({
      page: page,
      size: 15,
    })
  );
}
