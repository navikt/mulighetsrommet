import { getPublisertStatus } from "@/utils/Utils";
import { GjennomforingFilterType } from "@/pages/gjennomforing/filter";
import { GjennomforingService } from "@tiltaksadministrasjon/api-client";
import { useDownloadFile } from "@/api/useDownloadFile";

export function useDownloadGjennomforingerAsExcel(filter: GjennomforingFilterType) {
  const body = {
    search: filter.search || null,
    navEnheter: filter.navEnheter,
    tiltakstyper: filter.tiltakstyper,
    statuser: filter.statuser,
    gjennomforingTyper: filter.gjennomforingTyper,
    avtaleId: filter.avtale || null,
    arrangorer: filter.arrangorer,
    visMineGjennomforinger: filter.visMineGjennomforinger,
    sort: filter.sortering.sortString || null,
    publisert: getPublisertStatus(filter.publisert) ?? null,
  };

  const query = { page: filter.page, size: filter.pageSize };

  return useDownloadFile(() =>
    GjennomforingService.lastNedGjennomforingerSomExcel({ body, query }),
  );
}
