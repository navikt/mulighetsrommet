import { useQuery } from "@tanstack/react-query";
import { useModiaContext } from "./useModiaContext";
import { QueryKeys } from "@/api/query-keys";
import { Bruker, BrukerService } from "mulighetsrommet-api-client";

export function useHentBrukerdata() {
  const { fnr } = useModiaContext();

  const requestBody = { norskIdent: fnr };

  return useQuery<Bruker>({
    queryKey: QueryKeys.Bruker(fnr),
    queryFn: () => BrukerService.getBrukerdata({ requestBody }),
  });
}
