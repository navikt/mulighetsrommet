import { useSuspenseQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../client";
import { QueryKeys } from "../query-keys";

export function useInnsatsgrupper() {
  return useSuspenseQuery({
    queryKey: QueryKeys.sanity.innsatsgrupper,
    queryFn: () => mulighetsrommetClient.veilederTiltak.getInnsatsgrupper(),
  });
}
