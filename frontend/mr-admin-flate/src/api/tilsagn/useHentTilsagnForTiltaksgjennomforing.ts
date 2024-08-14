import { useQuery } from "@tanstack/react-query";
import { TilsagnService } from "@mr/api-client";
import { QueryKeys } from "../QueryKeys";

export function useHentTilsagnForTiltaksgjennomforing(tiltaksgjennomforingId?: string) {
  return useQuery({
    queryFn: () =>
      TilsagnService.tilsagnByTiltaksgjennomforing({
        tiltaksgjennomforingId: tiltaksgjennomforingId!,
      }),
    queryKey: QueryKeys.getTilsagnForGjennomforing(tiltaksgjennomforingId!),
    enabled: !!tiltaksgjennomforingId,
  });
}
