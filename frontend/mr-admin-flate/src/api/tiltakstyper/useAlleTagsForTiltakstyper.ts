import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useAlleTagsForTiltakstyper() {
  return useQuery(QueryKeys.tags, () =>
    mulighetsrommetClient.tiltakstyper.hentAlleTags()
  );
}
