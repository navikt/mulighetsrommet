import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { useState } from "react";
import { GjennomforingHandling, TilsagnType } from "@tiltaksadministrasjon/api-client";
import { useGjennomforingHandlinger } from "@/api/gjennomforing/useGjennomforing";
import { useTilsagnTableData } from "@/pages/gjennomforing/tilsagn/detaljer/tilsagnDetaljerLoader";
import { TilsagnTable } from "@/pages/gjennomforing/tilsagn/tabell/TilsagnTable";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { PagineringContainer } from "@/components/paginering/PagineringContainer";
import { PagineringsOversikt } from "@/components/paginering/PagineringOversikt";
import { Pagination } from "@navikt/ds-react";
import { useFilterState } from "@/filter/useFilterState";
import { tilsagnFilterStateAtom } from "@/pages/gjennomforing/tilsagn/filter";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { ContentBox } from "@/layouts/ContentBox";
import { ListSkeleton } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { Suspense } from "react";
import { TilsagnFilter } from "@/components/filter/TilsagnFilter";
import { TilsagnFilterTags } from "@/components/filter/TilsagnFilterTags";

export function TilsagnForGjennomforingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const handlinger = useGjennomforingHandlinger(gjennomforingId);
  const { filter, updateFilter, hasChanged, resetToDefault } =
    useFilterState(tilsagnFilterStateAtom);
  const { data: tilsagn } = useTilsagnTableData(gjennomforingId, filter.values);

  const [filterOpen, setFilterOpen] = useState(false);

  return (
    <>
      <ContentBox>
        <FilterAndTableLayout
          hasChanged={hasChanged}
          filter={
            <TilsagnFilter
              filter={filter.values}
              tilgjengeligeKostnadssteder={tilsagn.kostnadssteder}
              updateFilter={updateFilter}
            />
          }
          nullstillFilterButton={<NullstillFilterKnapp onClick={resetToDefault} />}
          tags={
            <TilsagnFilterTags
              filter={filter.values}
              updateFilter={updateFilter}
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
            <Suspense fallback={<ListSkeleton />}>
              <TilsagnTable
                emptyStateMessage="Det finnes ingen tilsagn for dette tiltaket"
                data={tilsagn.table}
              />
            </Suspense>
          }
          filterOpen={filterOpen}
          setFilterOpen={setFilterOpen}
        />
      </ContentBox>
      <TilToppenKnapp />
      <PagineringContainer>
        <PagineringsOversikt
          page={filter.values.page}
          pageSize={tilsagn.pagination.pageSize}
          antall={tilsagn.table.rows.length}
          maksAntall={tilsagn.pagination.totalCount}
          type="tilsagn"
          onChangePageSize={(size) => {
            updateFilter({
              page: 1,
              pageSize: size,
            });
          }}
        />
        <Pagination
          size="small"
          page={filter.values.page}
          count={tilsagn.pagination.totalPages}
          onPageChange={(page) => {
            updateFilter({ page });
          }}
          data-version="v1"
        />
      </PagineringContainer>
    </>
  );
}
