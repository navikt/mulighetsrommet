import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingerService } from "@mr/api-client";

export function useGjennomforingEndringshistorikk(id: string) {
  return useSuspenseQuery({
    queryKey: QueryKeys.gjennomforingHistorikk(id),
    queryFn() {
      return GjennomforingerService.getGjennomforingEndringshistorikk({
        id,
      });
    },
  });
}
