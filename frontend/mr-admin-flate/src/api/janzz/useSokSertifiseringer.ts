import { useQuery } from "@tanstack/react-query";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { JanzzService } from "mulighetsrommet-api-client";

export function useSokSertifiseringer(q: string) {
  const debouncedSok = useDebounce(q, 300);

  return useQuery({
    queryKey: QueryKeys.sokSertifiseringer(debouncedSok),
    queryFn: () =>
      JanzzService.sokSertifiseringer({
        q: debouncedSok.trim(),
      }),
    enabled: !!debouncedSok,
  });
}
