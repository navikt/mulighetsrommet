import { useQuery } from "@tanstack/react-query";
import { UtkastRequest as Utkast } from "mulighetsrommet-api-client";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useMineUtkast(utkasttype: Utkast.type) {
  const avtaleId = useGetAvtaleIdFromUrl();

  return useQuery({
    queryKey: QueryKeys.mineUtkast(avtaleId, utkasttype),

    queryFn: () =>
      mulighetsrommetClient.utkast.getMineUtkast({
        utkasttype,
        avtaleId,
      }),
  });
}
