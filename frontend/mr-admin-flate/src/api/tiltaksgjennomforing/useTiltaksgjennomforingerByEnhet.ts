import { useQuery } from "@tanstack/react-query";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { paginationAtom } from "../atoms";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useTiltaksgjennomforingerByEnhet(enhet: string = "") {
  const [page] = useAtom(paginationAtom);

  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforingerByEnhet(enhet, page),
    queryFn: () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getAllByEnhet({
        enhet,
        page,
        size: PAGE_SIZE,
      }),
    enabled: !!enhet,
  });
}
