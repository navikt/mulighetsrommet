import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useEnheter() {
  const { tiltakstypeId = null } = useParams<{ tiltakstypeId: string }>();

  return useQuery(QueryKeys.enheter(), () => {
    return mulighetsrommetClient.hentEnheter.hentEnheter({ tiltakstypeId });
  });
}
