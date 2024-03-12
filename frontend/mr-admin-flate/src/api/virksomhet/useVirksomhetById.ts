import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useVirksomhetById(id: string) {
  return useQuery({
    queryKey: QueryKeys.virksomhet(id),
    queryFn: () => {
      return mulighetsrommetClient.virksomhet.getVirksomhetById({ id });
    },
  });
}
