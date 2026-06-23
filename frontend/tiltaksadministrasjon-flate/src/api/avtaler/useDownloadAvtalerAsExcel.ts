import { AvtaleFilterType } from "@/pages/avtaler/filter";
import { AvtaleService } from "@tiltaksadministrasjon/api-client";
import { useDownloadFile } from "@/api/useDownloadFile";

export function useDownloadAvtalerAsExcel(filter: AvtaleFilterType) {
  const body = {
    search: filter.sok || null,
    tiltakstyper: filter.tiltakstyper,
    statuser: filter.statuser,
    avtaletyper: filter.avtaletyper,
    navEnheter: filter.navEnheter,
    arrangorer: filter.arrangorer,
    personvernBekreftet: filter.personvernBekreftet ?? null,
    visMineAvtaler: filter.visMineAvtaler,
    sort: filter.sortering.sortString || null,
  };

  return useDownloadFile(() => AvtaleService.lastNedAvtalerSomExcel({ body }));
}
