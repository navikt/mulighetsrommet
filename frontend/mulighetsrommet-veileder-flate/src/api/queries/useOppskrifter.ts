import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { erPreview } from "@/utils/Utils";
import { OppskrifterService } from "mulighetsrommet-api-client";

export function useOppskrifter(tiltakstypeId?: string) {
  return useQuery({
    queryKey: QueryKeys.oppskrifter(tiltakstypeId!!),
    queryFn: () =>
      OppskrifterService.getOppskrifter({
        tiltakstypeId: tiltakstypeId!!,
        perspective: erPreview() ? "previewDrafts" : "published",
      }),
    enabled: !!tiltakstypeId,
  });
}
