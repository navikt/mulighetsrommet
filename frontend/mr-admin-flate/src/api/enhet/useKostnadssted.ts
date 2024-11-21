import { useQuery } from "@tanstack/react-query";
import { NavEnheterService } from "@mr/api-client";
import { QueryKeys } from "@/api/QueryKeys";

export function useKostnadssted(regioner: string[] = []) {
  return useQuery({
    queryKey: QueryKeys.kostnadssted(regioner),

    queryFn: () => {
      return NavEnheterService.getKostnadssted({ regioner });
    },
  });
}
