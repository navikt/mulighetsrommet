import { TabellWrapper } from "@/components/tabell/TabellWrapper";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { Table } from "@navikt/ds-react";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { tiltakstypeFilterStateAtom } from "@/pages/tiltakstyper/filter";
import { useFilterState } from "@/filter/useFilterState";

export function TiltakstypeTable() {
  const { filter, updateFilter } = useFilterState(tiltakstypeFilterStateAtom);

  const sort = filter.values.sort?.tableSort;

  const handleSort = (sortKey: string) => {
    const direction = sort?.direction === "ascending" ? "descending" : "ascending";

    updateFilter({
      sort: {
        sortString: `${sortKey}-${direction}`,
        tableSort: {
          orderBy: sortKey,
          direction,
        },
      },
    });
  };

  const tiltakstyper = useTiltakstyper(filter.values);

  return (
    <TabellWrapper className="m-0">
      <Table
        sort={sort}
        onSortChange={(sortKey) => handleSort(sortKey)}
        className="bg-ax-bg-default border-separate border-spacing-0 border-t border-ax-neutral-300"
      >
        <Table.Header>
          <Table.Row>
            {headers.map((header) => (
              <Table.ColumnHeader
                key={header.sortKey}
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
  sortKey: "navn" | "tiltakskode" | "tiltaksgruppe";
  tittel: string;
  sortable: boolean;
  width: string;
}

const headers: ColumnHeader[] = [
  {
    sortKey: "navn",
    tittel: "Navn",
    sortable: true,
    width: "3fr",
  },
  {
    sortKey: "tiltakskode",
    tittel: "Tiltakskode",
    sortable: true,
    width: "1fr",
  },
  {
    sortKey: "tiltaksgruppe",
    tittel: "Tiltaksgruppe",
    sortable: true,
    width: "1fr",
  },
];
