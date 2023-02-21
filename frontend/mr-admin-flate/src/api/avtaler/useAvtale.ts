import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useAvtale() {
  const { avtaleId } = useParams<{ avtaleId: string }>();
  if (!avtaleId) {
    throw new Error("Fant ingen avtaleId i url");
  }

  return useQuery(QueryKeys.avtale(avtaleId), () =>
    mulighetsrommetClient.avtaler.getAvtale({ id: avtaleId })
  );
}
