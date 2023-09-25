import { useQuery } from "@tanstack/react-query";
import { VirksomhetTil } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useVirksomheter(til?: VirksomhetTil) {
  return useQuery(QueryKeys.virksomheter(til), () => {
    return mulighetsrommetClient.virksomhet.getVirksomheter({
      til,
    });
  });
}
