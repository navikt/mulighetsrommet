import { useQuery } from "@tanstack/react-query";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";

export function useTypeahead(q: string, domene: string) {
  const debouncedSok = useDebounce(q, 300);

  return useQuery({
    queryKey: QueryKeys.typeahead(debouncedSok, domene),
    queryFn: () =>
      mulighetsrommetClient.typeahead.typeahead({
        domene,
        q: debouncedSok.trim(),
      }),
    enabled: !!debouncedSok,
  });
}
