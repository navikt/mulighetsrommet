import { useQuery } from "@tanstack/react-query";
import { Bruker } from "mulighetsrommet-api-client";
import { useAppContext } from "../../../hooks/useAppContext";
import { erPreview } from "../../../utils/Utils";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";

export function useHentBrukerdata() {
  const { fnr } = useAppContext();

  const requestBody = { norskIdent: fnr };

  return useQuery<Bruker>({
    queryKey: [QueryKeys.Brukerdata, fnr],
    queryFn: () => mulighetsrommetClient.bruker.getBrukerdata({ requestBody }),
    enabled: !erPreview(),
  });
}
