import { ArrangorflateTilsagnDto, TilsagnType } from "api-client";
import { Alert, Table } from "@navikt/ds-react";
import { LinkWithTabState } from "../common/LinkWithTabState";
import { TilsagnStatusTag } from "./TilsagnStatusTag";
import { useOrgnrFromUrl, pathByOrgnr } from "~/utils/navigation";
import { sortBy, SortBySelector } from "~/utils/sort-by";
import { useSortState } from "~/hooks/useSortState";
import { formaterPeriode } from "@mr/frontend-common/utils/date";

interface Props {
  tilsagn: ArrangorflateTilsagnDto[];
}
enum TilsagnSortKey {
  TILSAGNSNUMMER = "TILSAGNSNUMMER",
  PERIODE = "PERIODE",
  STATUS = "STATUS",
  TYPE = "TYPE",
}

function getTilsagnSelector(sortKey: TilsagnSortKey): SortBySelector<ArrangorflateTilsagnDto> {
  switch (sortKey) {
    case TilsagnSortKey.PERIODE:
      return (t) => t.periode.start;
    case TilsagnSortKey.TILSAGNSNUMMER:
      return (t) => t.bestillingsnummer;
    case TilsagnSortKey.STATUS:
      return (t) => t.status;
    case TilsagnSortKey.TYPE:
      return (t) => t.type;
  }
}

export function TilsagnTable({ tilsagn }: Props) {
  const orgnr = useOrgnrFromUrl();
  const { sort, handleSort } = useSortState<TilsagnSortKey>();

  if (tilsagn.length === 0) {
    return (
      <Alert className="my-10" variant="info">
        Det finnes ingen tilsagn her
      </Alert>
    );
  }

  const sortedData = sort
    ? sortBy(tilsagn, sort.direction, getTilsagnSelector(sort.orderBy))
    : tilsagn;

  return (
    <Table
      zebraStripes
      sort={sort}
      onSortChange={(sortKey) => handleSort(sortKey as TilsagnSortKey)}
    >
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortable sortKey={TilsagnSortKey.PERIODE}>
            Periode
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortable sortKey={TilsagnSortKey.TILSAGNSNUMMER}>
            Tilsagnsnummer
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortable sortKey={TilsagnSortKey.TYPE}>
            Tilsagnstype
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortable sortKey={TilsagnSortKey.STATUS}>
            Status
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Handlinger</Table.ColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map((tilsagn, i) => {
          return (
            <Table.Row key={i}>
              <Table.HeaderCell scope="row">{tilsagn.gjennomforing.navn}</Table.HeaderCell>
              <Table.DataCell className="whitespace-nowrap">
                {formaterPeriode(tilsagn.periode)}
              </Table.DataCell>
              <Table.DataCell>{tilsagn.bestillingsnummer}</Table.DataCell>
              <Table.DataCell>{formaterTilsagnType(tilsagn.type)}</Table.DataCell>
              <Table.DataCell>
                <TilsagnStatusTag status={tilsagn.status} />
              </Table.DataCell>
              <Table.DataCell>
                <LinkWithTabState
                  aria-label={`Se detaljer for tilsagn for ${tilsagn.gjennomforing.navn}`}
                  to={pathByOrgnr(orgnr).tilsagn(tilsagn.id)}
                >
                  Se detaljer
                </LinkWithTabState>
              </Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}

export function formaterTilsagnType(type: TilsagnType): string {
  switch (type) {
    case TilsagnType.TILSAGN:
      return "Tilsagn";
    case TilsagnType.EKSTRATILSAGN:
      return "Ekstratilsagn";
    case TilsagnType.INVESTERING:
      return "Tilsagn for investeringer";
  }
}
