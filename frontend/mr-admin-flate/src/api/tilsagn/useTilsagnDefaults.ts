import { useSuspenseQuery } from "@tanstack/react-query";
import { TilsagnService } from "@mr/api-client";

export function useTilsagnDefaults(gjennomforingId: string) {
  return useSuspenseQuery({
    queryFn: () => TilsagnService.getTilsagnDefaults({ gjennomforingId }),
    queryKey: ["tilsagn", "defaults", gjennomforingId],
  });
}
