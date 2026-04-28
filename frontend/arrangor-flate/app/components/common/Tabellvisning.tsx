import { Pagination, PaginationProps, SortState, Table, VStack } from "@navikt/ds-react";

export interface Kolonne {
  key: string;
  label: string;
  sortable?: boolean;
}

interface TabellvisningProps extends React.PropsWithChildren {
  kolonner: Array<Kolonne>;
  sort: SortState | undefined;
  onSortChange: (key: string) => void;
  pagination?: PaginationProps;
}

export function Tabellvisning({
  kolonner,
  children,
  sort,
  onSortChange,
  pagination,
}: TabellvisningProps) {
  return (
    <VStack gap="space-16" align="center">
      <Table sort={sort} onSortChange={onSortChange} zebraStripes>
        <Table.Header>
          <Table.Row>
            {kolonner.map((kolonne) => (
              <Table.ColumnHeader
                scope="col"
                sortable={kolonne.sortable}
                sortKey={kolonne.key}
                key={kolonne.key}
              >
                {kolonne.label}
              </Table.ColumnHeader>
            ))}
            <Table.ColumnHeader scope="col">Handlinger</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>{children}</Table.Body>
      </Table>
      {pagination && <Pagination {...pagination} />}
    </VStack>
  );
}
