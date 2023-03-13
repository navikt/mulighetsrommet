import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useNokkeltallForAvtale() {
  const { avtaleId } = useParams<{ avtaleId: string }>();
  if (!avtaleId) {
    throw new Error("Fant ingen avtaleId i url");
  }

  return useQuery(QueryKeys.nokkeltallAvtale(avtaleId), () =>
    mulighetsrommetClient.avtaler.getNokkeltallForAvtale({ id: avtaleId })
  );
}
