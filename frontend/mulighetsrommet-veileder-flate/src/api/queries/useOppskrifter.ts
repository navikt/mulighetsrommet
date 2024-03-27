import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { mulighetsrommetClient } from "../client";
import { erPreview } from "@/utils/Utils";

export function useOppskrifter(tiltakstypeId?: string) {
  return useQuery({
    queryKey: QueryKeys.oppskrifter(tiltakstypeId!!),
    queryFn: () =>
      mulighetsrommetClient.oppskrifter.getOppskrifter({
        tiltakstypeId: tiltakstypeId!!,
        perspective: erPreview() ? "previewDrafts" : "published",
      }),
    enabled: !!tiltakstypeId,
  });
}
