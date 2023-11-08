import { Bruker } from "mulighetsrommet-api-client";
import { QueryKeys } from "../query-keys";
import { useFnr } from "../../../hooks/useFnr";
import { mulighetsrommetClient } from "../clients";
import { erPreview } from "../../../utils/Utils";
import { useQuery } from "@tanstack/react-query";

export function useHentBrukerdata() {
  const fnr = useFnr();

  const requestBody = { norskIdent: fnr };

  return useQuery<Bruker>({
    queryKey: [QueryKeys.Brukerdata, fnr],
    queryFn: () => mulighetsrommetClient.bruker.getBrukerdata({ requestBody }),
    enabled: !erPreview(),
  });
}
