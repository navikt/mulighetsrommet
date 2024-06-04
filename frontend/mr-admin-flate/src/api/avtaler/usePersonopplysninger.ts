import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "@/api/QueryKeys";

export function usePersonopplysninger() {
  return useQuery({
    queryKey: QueryKeys.personopplysninger(),
    queryFn: () => mulighetsrommetClient.personopplysninger.getPersonopplysninger(),
  });
}
