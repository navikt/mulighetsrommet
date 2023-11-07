import { erPreview } from "../../../utils/Utils";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import { useQuery } from "@tanstack/react-query";

export function useHentVeilederdata() {
  return useQuery({
    queryKey: [QueryKeys.Veilederdata],
    queryFn: () => mulighetsrommetClient.veileder.getVeileder(),
    enabled: !erPreview(),
  });
}
