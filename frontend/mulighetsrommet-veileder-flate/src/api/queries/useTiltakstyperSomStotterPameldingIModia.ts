import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { TiltakstyperService } from "@mr/api-client";

export function useTiltakstyperSomSnartStotterPameldingIModia() {
  return useQuery({
    queryKey: QueryKeys.tiltakstyperSomSnartStotterPameldingIModia(),
    queryFn: () => TiltakstyperService.getTiltakstyperSomSnartHarPameldingImodia(),
  });
}
