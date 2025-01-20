import { useApiQuery } from "@/hooks/useApiQuery";
import { QueryKeys } from "../query-keys";
import { erPreview } from "@/utils/Utils";
import { OppskrifterService } from "@mr/api-client-v2";

export function useOppskrifter(tiltakstypeId?: string) {
  return useApiQuery({
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
