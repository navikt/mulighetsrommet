import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useUtkast(utkastId: string) {
  return useQuery(QueryKeys.utkast(utkastId), () =>
    mulighetsrommetClient.utkast.getUtkast({ id: utkastId })
  );
}
