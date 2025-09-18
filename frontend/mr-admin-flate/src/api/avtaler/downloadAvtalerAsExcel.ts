import { AvtaleFilterType } from "@/pages/avtaler/filter";
import { AvtaleService } from "@tiltaksadministrasjon/api-client";

export async function downloadAvtalerAsExcel(filter: AvtaleFilterType) {
  const query = {
    search: filter.sok,
    tiltakstyper: filter.tiltakstyper,
    statuser: filter.statuser,
    avtaletyper: filter.avtaletyper,
    navRegioner: filter.navRegioner,
    arrangorer: filter.arrangorer,
    personvernBekreftet: filter.personvernBekreftet,
    visMineAvtaler: filter.visMineAvtaler,
    size: 10000,
  };
  const { data } = await AvtaleService.lastNedAvtalerSomExcel({
    query,
  });
  return data;
}
