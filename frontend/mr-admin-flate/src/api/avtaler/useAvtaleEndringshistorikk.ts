import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { AvtalerService } from "@mr/api-client";

export function useAvtaleEndringshistorikk(id: string) {
  return useSuspenseQuery({
    queryKey: QueryKeys.avtaleHistorikk(id),
    queryFn() {
      return AvtalerService.getAvtaleEndringshistorikk({
        id,
      });
    },
  });
}
