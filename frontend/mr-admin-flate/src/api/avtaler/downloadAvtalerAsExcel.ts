import { AvtaleFilterType } from "@/pages/avtaler/filter";
import { AvtalerService, LastNedAvtalerSomExcelData } from "@mr/api-client-v2";

export async function downloadAvtalerAsExcel(filter: AvtaleFilterType) {
  const query = createQueryParamsForExcelDownloadForAvtale(filter);
  const { data } = await AvtalerService.lastNedAvtalerSomExcel(query);
  return data;
}

function createQueryParamsForExcelDownloadForAvtale(
  filter: AvtaleFilterType,
): Pick<LastNedAvtalerSomExcelData, "query"> {
  return {
    query: {
      search: filter.sok,
      tiltakstyper: filter.tiltakstyper,
      statuser: filter.statuser,
      avtaletyper: filter.avtaletyper,
      navRegioner: filter.navRegioner,
      arrangorer: filter.arrangorer,
      personvernBekreftet: filter.personvernBekreftet,
      visMineAvtaler: filter.visMineAvtaler,
      size: 10000,
    },
  };
}
