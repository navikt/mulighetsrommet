import { WritableAtom, useAtom } from "jotai";
import { ArrangorerFilter } from "../../api/atoms";
import { useArrangorer } from "../../api/arrangor/useArrangorer";
import { useSort } from "../../hooks/useSort";
import { ToolbarContainer } from "mulighetsrommet-frontend-common/components/toolbar/toolbarContainer/ToolbarContainer";
import { PagineringsOversikt } from "../paginering/PagineringOversikt";
import { TabellWrapper } from "./TabellWrapper";
import { Laster } from "../laster/Laster";
import { Alert, Table } from "@navikt/ds-react";
import { SorteringArrangorer } from "mulighetsrommet-api-client";
import { Link } from "react-router-dom";

interface Props {
  filterAtom: WritableAtom<ArrangorerFilter, [newValue: ArrangorerFilter], void>;
  tagsHeight: number;
  filterOpen: boolean;
}

export function ArrangorerTabell({ filterAtom, tagsHeight, filterOpen }: Props) {
  const [sort, setSort] = useSort("navn");
  const [filter, setFilter] = useAtom(filterAtom);

  const { data: arrangorer = [], isLoading } = useArrangorer(); // TODO Må hente ut arrangorer med paginert objekt

  function updateFilter(newFilter: Partial<ArrangorerFilter>) {
    setFilter({ ...filter, ...newFilter });
  }

  const handleSort = (sortKey: string) => {
    // Hvis man bytter sortKey starter vi med ascending
    const direction =
      sort.orderBy === sortKey
        ? sort.direction === "descending"
          ? "ascending"
          : "descending"
        : "ascending";

    setSort({
      orderBy: sortKey,
      direction,
    });

    updateFilter({
      sortering: `${sortKey}-${direction}` as SorteringArrangorer,
      page: sort.orderBy !== sortKey || sort.direction !== direction ? 1 : filter.page,
    });
  };

  if (!arrangorer || isLoading) {
    return <Laster size="xlarge" tekst="Laster arrangører..." />;
  }

  return (
    <>
      <ToolbarContainer tagsHeight={tagsHeight} filterOpen={filterOpen}>
        <PagineringsOversikt
          page={filter.page}
          pageSize={filter.pageSize}
          antall={arrangorer.length}
          maksAntall={20} // TODO Må bruke verdi fra pagineringsobjekt fra backend
          type="arrangører"
          onChangePageSize={(value) => {
            updateFilter({
              page: 1,
              pageSize: value,
            });
          }}
        />
      </ToolbarContainer>
      <TabellWrapper filterOpen={filterOpen}>
        {arrangorer.length === 0 ? (
          <Alert variant="info">Ingen arrangører funnet</Alert>
        ) : (
          <Table sort={sort!} onSortChange={(sortKey) => handleSort(sortKey!)}>
            <Table.Header
              style={{
                top: `calc(${tagsHeight}px + 7.4rem)`,
              }}
            >
              <Table.Row>
                {headers.map((header) => {
                  return (
                    <Table.ColumnHeader
                      key={header.sortKey}
                      sortKey={header.sortKey}
                      sortable={header.sortable}
                      style={{ width: header.width }}
                    >
                      {header.tittel}
                    </Table.ColumnHeader>
                  );
                })}
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {arrangorer.map((arrangor) => {
                return (
                  <Table.Row key={arrangor.id}>
                    <Table.DataCell>
                      <Link to={`${arrangor.id}`}>{arrangor.navn}</Link>
                    </Table.DataCell>
                    <Table.DataCell>{arrangor.organisasjonsnummer}</Table.DataCell>
                  </Table.Row>
                );
              })}
            </Table.Body>
          </Table>
        )}
      </TabellWrapper>
    </>
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
    tittel: "Arrangørnavn",
    sortable: true,
    width: "3fr",
  },
  {
    sortKey: "organisasjonsnummer",
    tittel: "Organisasjonsnummer",
    sortable: false,
    width: "1fr",
  },
];

type Kolonne = "navn" | "organisasjonsnummer";
