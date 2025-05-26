import { getPublisertStatus } from "@/utils/Utils";
import { GjennomforingerService, LastNedGjennomforingerSomExcelData } from "@mr/api-client-v2";
import { GjennomforingFilterType } from "@/pages/gjennomforing/filter";

export async function downloadGjennomforingerAsExcel(filter: GjennomforingFilterType) {
  const query = createQueryParamsForExcelDownloadForGjennomforing(filter);
  const { data } = await GjennomforingerService.lastNedGjennomforingerSomExcel(query);
  return data;
}

function createQueryParamsForExcelDownloadForGjennomforing(
  filter: GjennomforingFilterType,
): Pick<LastNedGjennomforingerSomExcelData, "query"> {
  return {
    query: {
      search: filter.search,
      navEnheter: filter.navEnheter.map((enhet) => enhet.enhetsnummer),
      tiltakstyper: filter.tiltakstyper,
      statuser: filter.statuser,
      avtaleId: filter.avtale,
      arrangorer: filter.arrangorer,
      visMineTiltaksgjennomforinger: filter.visMineGjennomforinger,
      size: filter.pageSize,
      sort: filter.sortering.sortString,
      publisert: getPublisertStatus(filter.publisert),
    },
  };
}
