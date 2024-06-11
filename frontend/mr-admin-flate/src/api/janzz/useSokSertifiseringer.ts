import { useQuery } from "@tanstack/react-query";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";

export function useSokSertifiseringer(q: string) {
  const debouncedSok = useDebounce(q, 300);

  return useQuery({
    queryKey: QueryKeys.sokSertifiseringer(debouncedSok),
    queryFn: () =>
      mulighetsrommetClient.janzz.sokSertifiseringer({
        q: debouncedSok.trim(),
      }),
    enabled: !!debouncedSok,
  });
}
