import { TabellWrapper } from "@/components/tabell/TabellWrapper";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { Table } from "@navikt/ds-react";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { tiltakstypeFilterStateAtom } from "@/pages/tiltakstyper/filter";
import { useFilterState } from "@/filter/useFilterState";
import { SortDirection, TiltakstypeSortField } from "@tiltaksadministrasjon/api-client";

export function TiltakstypeTable() {
  const { filter, updateFilter } = useFilterState(tiltakstypeFilterStateAtom);

  const sort = filter.values.sort;

  const handleSort = (field: TiltakstypeSortField) => {
    updateFilter({ sort: resolveSort(sort, field) });
  };

  const tiltakstyper = useTiltakstyper(filter.values);

  return (
    <TabellWrapper className="m-0">
      <Table
        sort={{
          orderBy: sort.field,
          direction: sort.direction === SortDirection.DESC ? "descending" : "ascending",
        }}
        onSortChange={(sortKey) => handleSort(sortKey as TiltakstypeSortField)}
        className="bg-ax-bg-default border-separate border-spacing-0 border-t border-ax-neutral-300"
      >
        <Table.Header>
          <Table.Row>
            {headers.map((header) => (
              <Table.ColumnHeader
                key={header.tittel}
                sortKey={header.sortKey}
                sortable={header.sortable}
                style={{
                  width: header.width,
                }}
              >
                {header.tittel}
              </Table.ColumnHeader>
            ))}
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {tiltakstyper.map((tiltakstype) => {
            return (
              <Table.Row key={tiltakstype.id}>
                <Table.DataCell
                  aria-label={`Navn på tiltakstype: ${tiltakstype.navn}`}
                  className="underline"
                >
                  <Lenke to={`/tiltakstyper/${tiltakstype.id}`}>{tiltakstype.navn}</Lenke>
                </Table.DataCell>
                <Table.DataCell>{tiltakstype.tiltakskode}</Table.DataCell>
                <Table.DataCell>{tiltakstype.gruppe}</Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </TabellWrapper>
  );
}

interface ColumnHeader {
  sortKey?: TiltakstypeSortField;
  tittel: string;
  sortable: boolean;
  width: string;
}

const headers: ColumnHeader[] = [
  {
    sortKey: TiltakstypeSortField.NAVN,
    tittel: "Navn",
    sortable: true,
    width: "3fr",
  },
  {
    sortKey: TiltakstypeSortField.TILTAKSKODE,
    tittel: "Tiltakskode",
    sortable: true,
    width: "1fr",
  },
  {
    tittel: "Tiltaksgruppe",
    sortable: false,
    width: "1fr",
  },
];

function resolveSort(
  current: { field: TiltakstypeSortField; direction: SortDirection },
  next: TiltakstypeSortField,
) {
  if (current.field !== next) {
    return { field: next, direction: SortDirection.ASC };
  }

  const direction =
    current.direction === SortDirection.ASC ? SortDirection.DESC : SortDirection.ASC;
  return { field: next, direction };
}
