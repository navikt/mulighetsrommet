import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";
import { useDebounce } from "mulighetsrommet-frontend-common";

export function useSokBrregEnheter(sokestreng: string) {
  const debouncedSok = useDebounce(sokestreng, 300);

  return useQuery(
    QueryKeys.brregSok(debouncedSok),
    () =>
      mulighetsrommetClient.virksomhet.sokVirksomhet({
        sok: debouncedSok.trim(),
      }),
    {
      enabled: !!debouncedSok,
    }
  );
}
