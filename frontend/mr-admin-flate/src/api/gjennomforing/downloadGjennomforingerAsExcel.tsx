import { getPublisertStatus } from "@/utils/Utils";
import { GjennomforingFilterType } from "@/pages/gjennomforing/filter";
import { GjennomforingService } from "@tiltaksadministrasjon/api-client";

export async function downloadGjennomforingerAsExcel(filter: GjennomforingFilterType) {
  const query = {
    search: filter.search,
    navEnheter: filter.navEnheter.map((enhet) => enhet.enhetsnummer),
    tiltakstyper: filter.tiltakstyper,
    statuser: filter.statuser,
    avtaleId: filter.avtale,
    arrangorer: filter.arrangorer,
    visMineGjennomforinger: filter.visMineGjennomforinger,
    size: filter.pageSize,
    sort: filter.sortering.sortString,
    publisert: getPublisertStatus(filter.publisert) ?? undefined,
  };
  const { data } = await GjennomforingService.lastNedGjennomforingerSomExcel({
    query,
  });
  return data;
}
