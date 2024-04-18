import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";

export function useArrangorHovedenhet(id: string) {
  return useQuery({
    queryKey: QueryKeys.arrangorHovedenhetById(id),
    queryFn: () => {
      return mulighetsrommetClient.arrangor.getArrangorHovedenhetById({ id });
    },
  });
}
