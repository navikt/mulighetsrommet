import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "../query-keys";

export function useTiltakstyperSomStotterPameldingIModia() {
  return useQuery({
    queryKey: QueryKeys.tiltakstyperSomStotterPameldingIModia(),
    queryFn: () => mulighetsrommetClient.tiltakstyper.getTiltakstyperSomStotterPameldingImodia(),
  });
}
