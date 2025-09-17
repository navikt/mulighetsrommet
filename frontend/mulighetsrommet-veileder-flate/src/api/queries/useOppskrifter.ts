import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "../query-keys";
import { erPreview } from "@/utils/Utils";
import { OppskrifterService, SanityPerspective } from "@api-client";

export function useOppskrifter(tiltakstypeId: string) {
  return useApiQuery({
    queryKey: QueryKeys.oppskrifter(tiltakstypeId),
    queryFn: () =>
      OppskrifterService.getOppskrifter({
        path: {
          tiltakstypeId,
        },
        query: {
          perspective: erPreview() ? SanityPerspective.PREVIEW_DRAFTS : SanityPerspective.PUBLISHED,
        },
      }),
    enabled: !!tiltakstypeId,
  });
}
