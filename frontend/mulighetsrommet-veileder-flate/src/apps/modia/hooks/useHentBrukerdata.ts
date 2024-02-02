import { useQuery } from "@tanstack/react-query";
import { Bruker } from "mulighetsrommet-api-client";
import { useModiaContext } from "./useModiaContext";
import { mulighetsrommetClient } from "@/core/api/clients";
import { QueryKeys } from "@/core/api/query-keys";

export function useHentBrukerdata() {
  const { fnr } = useModiaContext();

  const requestBody = { norskIdent: fnr };

  return useQuery<Bruker>({
    queryKey: [QueryKeys.Brukerdata, fnr],
    queryFn: () => mulighetsrommetClient.bruker.getBrukerdata({ requestBody }),
  });
}
