import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "../query-keys";
import { erPreview } from "@/utils/Utils";
import { OppskrifterService, SanityPerspective, Tiltakskode } from "@api-client";

export function useOppskrifter(tiltakskode: Tiltakskode) {
  return useApiQuery({
    queryKey: QueryKeys.oppskrifter(tiltakskode),
    queryFn: () =>
      OppskrifterService.getOppskrifter({
        path: {
          tiltakskode,
        },
        query: {
          perspective: erPreview() ? SanityPerspective.PREVIEW_DRAFTS : SanityPerspective.PUBLISHED,
        },
      }),
    enabled: !!tiltakskode,
  });
}
