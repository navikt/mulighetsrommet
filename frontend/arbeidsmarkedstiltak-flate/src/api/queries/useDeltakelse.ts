import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { HistorikkService } from "@arbeidsmarkedstiltak/api-client";
import { QueryKeys } from "../query-keys";
import { useTiltakIdFraUrl } from "@/hooks/useTiltakIdFraUrl";
import { useQuery } from "@tanstack/react-query";

export function useAktiveDeltakelser() {
  const { fnr: norskIdent } = useModiaContext();

  const tiltakId = useTiltakIdFraUrl();
  return useQuery({
    queryKey: QueryKeys.Deltakelse(norskIdent, tiltakId),
    queryFn: async () => {
      const { data } = await HistorikkService.hentDeltakelse({
        body: { norskIdent, tiltakId },
      });
      return data;
    },
    enabled: !!tiltakId,
  });
}
