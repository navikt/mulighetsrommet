import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { TiltakstyperService } from "mulighetsrommet-api-client";

export function useTiltakstyperSomStotterPameldingIModia() {
  return useQuery({
    queryKey: QueryKeys.tiltakstyperSomStotterPameldingIModia(),
    queryFn: () => TiltakstyperService.getTiltakstyperSomStotterPameldingImodia(),
  });
}
