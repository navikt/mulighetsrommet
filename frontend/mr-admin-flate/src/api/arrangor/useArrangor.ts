import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { ArrangorService } from "mulighetsrommet-api-client";

export function useArrangor(id: string) {
  return useQuery({
    queryKey: QueryKeys.arrangorById(id),
    queryFn: () => {
      return ArrangorService.getArrangorById({ id });
    },
  });
}
