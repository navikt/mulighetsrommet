import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { mulighetsrommetClient } from "../client";

export function useBehandlingAvPersonopplysningerFraAvtale(avtaleId?: string) {
  return useQuery({
    queryKey: QueryKeys.behandlingAvPersonopplysninger(avtaleId),
    queryFn: () =>
      mulighetsrommetClient.avtaler.getBehandlingAvPersonopplysninger({ id: avtaleId!! }),
    enabled: !!avtaleId,
  });
}
