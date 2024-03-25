import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useArrangor(id: string) {
  return useQuery({
    queryKey: QueryKeys.arrangorById(id),
    queryFn: () => {
      return mulighetsrommetClient.arrangor.getArrangorById({ id });
    },
  });
}
