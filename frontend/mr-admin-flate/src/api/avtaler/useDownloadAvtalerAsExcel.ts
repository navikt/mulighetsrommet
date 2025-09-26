import { AvtaleFilterType } from "@/pages/avtaler/filter";
import { AvtaleService } from "@tiltaksadministrasjon/api-client";
import { useDownloadFile } from "@/api/useDownloadFile";

export function useDownloadAvtalerAsExcel(filter: AvtaleFilterType) {
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

  return useDownloadFile(() => AvtaleService.lastNedAvtalerSomExcel({ query }));
}
