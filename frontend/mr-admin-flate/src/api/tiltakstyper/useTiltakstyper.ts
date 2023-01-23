import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { paginationAtom, tiltakstypefilter } from "../atoms";
import { TiltakstypeStatus } from "mulighetsrommet-api-client";

export function useTiltakstyper() {
  const [page] = useAtom(paginationAtom);
  const [sokefilter] = useAtom(tiltakstypefilter);
  return useQuery(
    QueryKeys.tiltakstyper(sokefilter.sok, sokefilter.status, page),
    () =>
      mulighetsrommetClient.tiltakstyper.getTiltakstyper({
        search: sokefilter.sok !== "" ? sokefilter.sok : undefined,
        status: sokefilter.status ?? TiltakstypeStatus.AKTIV,
        page,
        size: PAGE_SIZE,
      })
  );
}
