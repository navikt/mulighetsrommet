import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useEnheter() {
  const { tiltakstypeId } = useParams<{ tiltakstypeId: string }>();

  return useQuery(QueryKeys.enheter(), () => {
    return mulighetsrommetClient.hentEnheter.hentEnheterMedAvtale({
      tiltakstypeId,
    });
  });
}
