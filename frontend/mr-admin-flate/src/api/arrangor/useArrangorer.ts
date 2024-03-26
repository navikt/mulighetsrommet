import { useQuery } from "@tanstack/react-query";
import { ArrangorTil } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "@/api/QueryKeys";

export function useArrangorer(til?: ArrangorTil) {
  return useQuery({
    queryKey: QueryKeys.arrangorer(til),

    queryFn: () => {
      return mulighetsrommetClient.arrangor.getArrangorer({
        til,
      });
    },
  });
}
