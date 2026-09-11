import { getPublisertStatus } from "@/utils/Utils";
import { GjennomforingFilterType } from "@/pages/gjennomforing/filter";
import { FeatureToggle, GjennomforingService } from "@tiltaksadministrasjon/api-client";
import { useDownloadFile } from "@/api/useDownloadFile";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { splitGjennomforingStatuser } from "@/utils/filterUtils";

export function useDownloadGjennomforingerAsExcel(filter: GjennomforingFilterType) {
  const { data: enableEnkeltplassFilter } = useFeatureToggle(
    FeatureToggle.TILTAKSADMINISTRASJON_ENKELTPLASS_FILTER,
  );
  const { statuser, enkeltplassStatuser } = enableEnkeltplassFilter
    ? splitGjennomforingStatuser(filter.gjennomforingStatuser)
    : { statuser: filter.statuser, enkeltplassStatuser: [] };

  const body = {
    search: filter.search || null,
    navEnheter: filter.navEnheter,
    tiltakstyper: filter.tiltakstyper,
    statuser,
    enkeltplassStatuser,
    avtaleId: filter.avtale || null,
    arrangorer: filter.arrangorer,
    visMineGjennomforinger: filter.visMineGjennomforinger,
    sort: filter.sortering.sortString || null,
    publisert: getPublisertStatus(filter.publisert) ?? null,
  };

  return useDownloadFile(() => GjennomforingService.lastNedGjennomforingerSomExcel({ body }));
}
