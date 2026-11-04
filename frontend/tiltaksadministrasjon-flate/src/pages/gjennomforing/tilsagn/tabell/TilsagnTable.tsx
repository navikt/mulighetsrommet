import { Heading, InfoCard } from "@navikt/ds-react";
import {
  useAktiveTilsagnTableData,
  useTilsagnTableData,
} from "@/pages/gjennomforing/tilsagn/detaljer/tilsagnDetaljerLoader";
import { DataDrivenTable } from "@mr/frontend-common";
import { InformationSquareIcon } from "@navikt/aksel-icons";
import { PagineringContainer } from "@/components/paginering/PagineringContainer";
import { Pagination } from "@navikt/ds-react";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { PagineringsOversikt } from "@/components/paginering/PagineringOversikt";
import { tilsagnFilterStateAtomFamily } from "../filter";
import { useFilterState } from "@/filter/useFilterState";

interface TilsagnTableProps {
  emptyStateMessage: string;
  gjennomforingId: string;
}

export function TilsagnTable({ emptyStateMessage, gjennomforingId }: TilsagnTableProps) {
  const { filter, updateFilter } = useFilterState(tilsagnFilterStateAtomFamily(gjennomforingId));
  const { data } = useTilsagnTableData(gjennomforingId, filter.values);
  return data.table.rows.length > 0 ? (
    <>
      <DataDrivenTable data={data.table} />
      <TilToppenKnapp />
      <PagineringContainer>
        <PagineringsOversikt
          page={filter.values.page}
          pageSize={data.pagination.pageSize}
          antall={data.table.rows.length}
          maksAntall={data.pagination.totalCount}
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
          count={data.pagination.totalPages}
          onPageChange={(page) => {
            updateFilter({ page });
          }}
          data-version="v1"
        />
      </PagineringContainer>
    </>
  ) : (
    <InfoCard data-color="info" className="mt-4">
      <InfoCard.Message icon={<InformationSquareIcon aria-hidden />}>
        {emptyStateMessage}
      </InfoCard.Message>
    </InfoCard>
  );
}

interface AktiveTilsagnTableProps {
  gjennomforingId: string;
}

export function AktiveTilsagnTable({ gjennomforingId }: AktiveTilsagnTableProps) {
  const { data } = useAktiveTilsagnTableData(gjennomforingId);
  return (
    <>
      <Heading size="medium">Aktive tilsagn</Heading>
      {data.table.rows.length > 0 ? (
        <DataDrivenTable data={data.table} />
      ) : (
        <InfoCard data-color="info" className="mt-4">
          <InfoCard.Message icon={<InformationSquareIcon aria-hidden />}>
            Det finnes ingen aktive tilsagn for dette tiltaket
          </InfoCard.Message>
        </InfoCard>
      )}
    </>
  );
}
