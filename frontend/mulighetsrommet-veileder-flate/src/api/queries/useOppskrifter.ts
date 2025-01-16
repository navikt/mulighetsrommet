import { useQueryWrapper } from "@/hooks/useQueryWrapper";
import { QueryKeys } from "../query-keys";
import { erPreview } from "@/utils/Utils";
import { OppskrifterService } from "@mr/api-client-v2";

export function useOppskrifter(tiltakstypeId?: string) {
  return useQueryWrapper({
    queryKey: QueryKeys.oppskrifter(tiltakstypeId!),
    queryFn: () =>
      OppskrifterService.getOppskrifter({
        path: {
          tiltakstypeId: tiltakstypeId!,
          perspective: erPreview() ? "previewDrafts" : "published",
        },
      }),
    enabled: !!tiltakstypeId,
  });
}
