import { TabellWrapper } from "@/components/tabell/TabellWrapper";
import { formaterDato } from "@/utils/Utils";
import { SorteringTiltakstyper } from "@mr/api-client-v2";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { Table } from "@navikt/ds-react";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { TiltakstypeStatusTag } from "@/components/statuselementer/TiltakstypeStatusTag";
import { tiltakstypeFilterStateAtom } from "@/pages/tiltakstyper/filter";
import { useFilterState } from "@/filter/useFilterState";

export function TiltakstypeTabell() {
  const { filter, updateFilter } = useFilterState(tiltakstypeFilterStateAtom);

  const sort = filter.values.sort?.tableSort;

  const handleSort = (sortKey: string) => {
    const direction = sort?.direction === "ascending" ? "descending" : "ascending";

    updateFilter({
      sort: {
        sortString: `${sortKey}-${direction}` as SorteringTiltakstyper,
        tableSort: {
          orderBy: sortKey,
          direction,
        },
      },
    });
  };

  const { data: tiltakstyper } = useTiltakstyper(filter.values);

  return (
    <TabellWrapper className="m-0">
      <Table
        sort={sort}
        onSortChange={(sortKey) => handleSort(sortKey)}
        className="bg-white border-separate border-spacing-0 border-t border-gray-200"
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
            const startDato = formaterDato(tiltakstype.startDato);
            const sluttDato = tiltakstype.sluttDato ? formaterDato(tiltakstype.sluttDato) : "-";
            return (
              <Table.Row key={tiltakstype.id}>
                <Table.DataCell
                  aria-label={`Navn på tiltakstype: ${tiltakstype.navn}`}
                  className="underline"
                >
                  <Lenke to={`/tiltakstyper/${tiltakstype.id}`}>{tiltakstype.navn}</Lenke>
                </Table.DataCell>
                <Table.DataCell aria-label={`Startdato: ${startDato}`}>{startDato}</Table.DataCell>
                <Table.DataCell aria-label={`Sluttdato: ${sluttDato}`}>{sluttDato}</Table.DataCell>
                <Table.DataCell>
                  <TiltakstypeStatusTag status={tiltakstype.status} />
                </Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </TabellWrapper>
  );
}

interface ColumnHeader {
  sortKey: Kolonne;
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
    sortKey: "startdato",
    tittel: "Startdato",
    sortable: true,
    width: "1fr",
  },
  {
    sortKey: "sluttdato",
    tittel: "Sluttdato",
    sortable: true,
    width: "1fr",
  },
  {
    sortKey: "status",
    tittel: "Status",
    sortable: false,
    width: "1fr",
  },
];

type Kolonne =
  | "navn"
  | "avtalenummer"
  | "arrangor"
  | "region"
  | "startdato"
  | "sluttdato"
  | "status";
