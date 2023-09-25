import { useQuery } from "@tanstack/react-query";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useSokVirksomheter(sokestreng: string) {
  const debouncedSok = useDebounce(sokestreng, 300);

  return useQuery(
    QueryKeys.virksomhetSok(debouncedSok),
    () =>
      mulighetsrommetClient.virksomhet.sokVirksomhet({
        sok: debouncedSok.trim(),
      }),
    {
      enabled: !!debouncedSok,
    },
  );
}
