import { useQuery } from "@tanstack/react-query";
import { PrismodellService } from "@mr/api-client";
import { QueryKeys } from "../QueryKeys";

export function useAvtalteSatser(avtaleId: string) {
  return useQuery({
    queryFn: () => PrismodellService.getAvtalteSatser({ avtaleId }),
    queryKey: QueryKeys.avtalteSatser(avtaleId),
  });
}
