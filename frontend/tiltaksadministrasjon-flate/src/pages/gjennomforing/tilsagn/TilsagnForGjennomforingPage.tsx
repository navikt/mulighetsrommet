import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { useState } from "react";
import { GjennomforingHandling, TilsagnType } from "@tiltaksadministrasjon/api-client";
import { useGjennomforingHandlinger } from "@/api/gjennomforing/useGjennomforing";
import { TilsagnTable } from "@/pages/gjennomforing/tilsagn/tabell/TilsagnTable";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { useFilterState } from "@/filter/useFilterState";
import {
  tilsagnFilterStateAtomFamily,
  type TilsagnFilterType,
} from "@/pages/gjennomforing/tilsagn/filter";
import { ContentBox } from "@/layouts/ContentBox";
import { ListSkeleton } from "@mr/frontend-common";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { Suspense } from "react";
import { TilsagnFilter } from "@/components/filter/TilsagnFilter";
import { TilsagnFilterTags } from "@/components/filter/TilsagnFilterTags";
import type { FilterKilde } from "@/sporing/useUmami";
import { useFilterTracking } from "@/sporing/useFilterTracking";
import { useKostnadsstedTracking } from "@/sporing/useKostnadsstedTracking";

export function TilsagnForGjennomforingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const handlinger = useGjennomforingHandlinger(gjennomforingId);
  const { filter, updateFilter, hasChanged, resetToDefault } = useFilterState(
    tilsagnFilterStateAtomFamily(gjennomforingId),
  );
  const { logFilterEndret, logFilterpanelÅpnet } = useFilterTracking("tilsagn");
  const { logKostnadsstederEndret } = useKostnadsstedTracking("tilsagn");

  const [filterOpen, setFilterOpen] = useState(false);

  const logStatusEndringer = (
    forrigeStatuser: TilsagnFilterType["statuser"],
    nesteStatuser: TilsagnFilterType["statuser"],
    kilde: FilterKilde,
  ) => {
    for (const status of new Set([...forrigeStatuser, ...nesteStatuser])) {
      const varValgt = forrigeStatuser.includes(status);
      const erValgt = nesteStatuser.includes(status);
      if (varValgt !== erValgt) {
        logFilterEndret({
          kategori: "Status",
          filternavn: status,
          handling: erValgt ? "valgt" : "fjernet",
          kilde,
        });
      }
    }
  };

  const updateFilterWithTracking = (
    values: Partial<TilsagnFilterType>,
    kilde: FilterKilde = "enkeltvalg",
  ) => {
    const forrigeFilter = filter.values;
    const nesteFilter = { ...forrigeFilter, ...values };
    updateFilter(values);
    logStatusEndringer(forrigeFilter.statuser, nesteFilter.statuser, kilde);
  };

  const handleResetFilter = () => {
    const forrigeFilter = filter.values;
    resetToDefault();
    logStatusEndringer(forrigeFilter.statuser, [], "nullstill");
    logKostnadsstederEndret(forrigeFilter.kostnadssteder, [], "nullstill");
  };

  const handleSetFilterOpen = (open: boolean) => {
    setFilterOpen(open);
    if (open) {
      logFilterpanelÅpnet();
    }
  };

  return (
    <>
      <ContentBox>
        <FilterAndTableLayout
          hasChanged={hasChanged}
          filter={
            <TilsagnFilter
              filter={filter.values}
              updateFilter={updateFilterWithTracking}
              gjennomforingId={gjennomforingId}
            />
          }
          nullstillFilterButton={<NullstillFilterKnapp onClick={handleResetFilter} />}
          tags={
            <TilsagnFilterTags
              filter={filter.values}
              updateFilter={updateFilterWithTracking}
              filterOpen={filterOpen}
            />
          }
          buttons={
            <Handlinger
              handlinger={handlinger}
              grupper={[
                {
                  items: [
                    {
                      label: `Opprett ${avtaletekster.tilsagn.type(TilsagnType.TILSAGN).toLowerCase()}`,
                      href: `opprett-tilsagn?type=${TilsagnType.TILSAGN}`,
                      handling: GjennomforingHandling.OPPRETT_TILSAGN,
                    },
                    {
                      label: `Opprett ${avtaletekster.tilsagn.type(TilsagnType.EKSTRATILSAGN).toLowerCase()}`,
                      href: `opprett-tilsagn?type=${TilsagnType.EKSTRATILSAGN}`,
                      handling: GjennomforingHandling.OPPRETT_EKSTRATILSAGN,
                    },
                    {
                      label: `Opprett ${avtaletekster.tilsagn.type(TilsagnType.INVESTERING).toLowerCase()}`,
                      href: `opprett-tilsagn?type=${TilsagnType.INVESTERING}`,
                      handling: GjennomforingHandling.OPPRETT_TILSAGN_FOR_INVESTERINGER,
                    },
                  ],
                },
              ]}
            />
          }
          table={
            <ReloadAppErrorBoundary>
              <Suspense fallback={<ListSkeleton />}>
                <TilsagnTable
                  emptyStateMessage="Det finnes ingen tilsagn for dette tiltaket"
                  gjennomforingId={gjennomforingId}
                />
              </Suspense>
            </ReloadAppErrorBoundary>
          }
          filterOpen={filterOpen}
          setFilterOpen={handleSetFilterOpen}
        />
      </ContentBox>
    </>
  );
}
