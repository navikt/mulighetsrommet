import { useSuspenseQuery } from "@tanstack/react-query";
import { useModiaContext } from "./useModiaContext";
import { QueryKeys } from "@/api/query-keys";
import { Bruker, BrukerService } from "@mr/api-client";

export function useHentBrukerdata() {
  const { fnr: norskIdent } = useModiaContext();

  return useSuspenseQuery<Bruker>({
    queryKey: QueryKeys.Bruker(norskIdent),
    queryFn: () => BrukerService.getBrukerdata({ requestBody: { norskIdent } }),
  });
}
