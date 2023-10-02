import { useQuery } from "react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";

export function useInnsatsgrupper() {
  return useQuery(QueryKeys.sanity.innsatsgrupper, () =>
    mulighetsrommetClient.sanity.getInnsatsgrupper(),
  );
}
