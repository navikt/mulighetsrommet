import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { ArrangorService } from "mulighetsrommet-api-client";

export function useArrangorHovedenhet(id: string) {
  return useQuery({
    queryKey: QueryKeys.arrangorHovedenhetById(id),
    queryFn: () => {
      return ArrangorService.getArrangorHovedenhetById({ id });
    },
  });
}
