import { useSuspenseQuery } from "@tanstack/react-query";
import { TilsagnService, TilsagnType } from "@mr/api-client";

export function useTilsagnDefaults(gjennomforingId: string, type: TilsagnType) {
  return useSuspenseQuery({
    queryFn: () => TilsagnService.getTilsagnDefaults({ gjennomforingId , type }),
    queryKey: ["tilsagn", "defaults", gjennomforingId],
  });
}
