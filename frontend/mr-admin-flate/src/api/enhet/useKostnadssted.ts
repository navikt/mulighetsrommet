import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { NavEnheterService } from "@mr/api-client";

export function useKostnadssted(regioner: string[] = []) {
  return useQuery({
    queryKey: QueryKeys.kostnadssted(regioner),

    queryFn: () => {
      return NavEnheterService.getKostnadssted({ regioner });
    },
  });
}
