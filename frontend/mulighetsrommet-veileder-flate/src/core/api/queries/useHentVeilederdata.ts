import { NavVeileder } from "mulighetsrommet-api-client";
import { useQuery } from "react-query";
import { erPreview } from "../../../utils/Utils";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";

export function useHentVeilederdata() {
  return useQuery<NavVeileder, Error>(
    [QueryKeys.Veilederdata],
    () => mulighetsrommetClient.veileder.getVeileder(),
    {
      enabled: !erPreview(),
    },
  );
}
