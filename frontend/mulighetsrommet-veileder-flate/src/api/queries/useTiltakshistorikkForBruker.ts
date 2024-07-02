import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/query-keys";
import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { useFeatureToggle } from "@/api/feature-toggles";
import { HistorikkService, Toggles } from "mulighetsrommet-api-client";

export function useTiltakshistorikkForBruker() {
  const { fnr } = useModiaContext();

  const { data: enableV2Historikk } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_ENABLE_V2HISTORIKK,
  );

  const requestBody = { norskIdent: fnr };

  const queryFn = enableV2Historikk
    ? () => HistorikkService.hentHistorikkForBrukerV2({ requestBody })
    : () => HistorikkService.hentHistorikkForBruker({ requestBody });

  return useQuery({
    queryKey: QueryKeys.BrukerHistorikk(enableV2Historikk, fnr),
    queryFn,
  });
}
