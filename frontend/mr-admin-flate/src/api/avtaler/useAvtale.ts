import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import invariant from "tiny-invariant";

export function useAvtale(overstyrAvtaleId?: string) {
  const avtaleId = overstyrAvtaleId || useGetAvtaleIdFromUrl();
  invariant(avtaleId, "Klarte ikke hente id for avtale.");

  return useQuery(
    QueryKeys.avtale(avtaleId!!),
    () => mulighetsrommetClient.avtaler.getAvtale({ id: avtaleId }),
    {},
  );
}
