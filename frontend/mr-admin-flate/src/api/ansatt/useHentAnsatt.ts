import { useQuery } from "@tanstack/react-query";
import { NavAnsatt } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "@/api/QueryKeys";

export function useHentAnsatt() {
  return useQuery<NavAnsatt, Error>({
    queryKey: QueryKeys.ansatt(),
    queryFn: () => mulighetsrommetClient.ansatt.hentInfoOmAnsatt(),
  });
}
