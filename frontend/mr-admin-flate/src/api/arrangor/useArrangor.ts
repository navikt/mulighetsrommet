import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";

export function useArrangor(id: string) {
  return useQuery({
    queryKey: QueryKeys.arrangorById(id),
    queryFn: () => {
      return mulighetsrommetClient.arrangor.getArrangorById({ id });
    },
  });
}
