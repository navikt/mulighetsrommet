import { useQuery } from "@tanstack/react-query";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { useHentAnsatt } from "../ansatt/useHentAnsatt";
import { paginationAtom } from "../atoms";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useTiltaksgjennomforingerByInnloggetAnsatt() {
  const { data: ansatt } = useHentAnsatt();
  const ansattId = ansatt?.ident;
  const [page] = useAtom(paginationAtom);

  return useQuery(
    QueryKeys.tiltaksgjennomforingerByAnsatt(ansattId, page),
    () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getAnsattsGjennomforinger({
        ansattId,
        page,
        size: PAGE_SIZE,
      }),
    {
      enabled: !!ansattId,
    }
  );
}
