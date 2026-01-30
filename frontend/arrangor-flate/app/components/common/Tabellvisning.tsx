import { SortState, Table } from "@navikt/ds-react";
interface TabellvisningProps extends React.PropsWithChildren {
  kolonner: Array<{ key: string; label: string }>;
  sort: SortState | undefined;
  onSortChange: (key: string) => void;
}

export function Tabellvisning({ kolonner, children, sort, onSortChange }: TabellvisningProps) {
  return (
    <Table sort={sort} onSortChange={onSortChange} zebraStripes>
      <Table.Header>
        <Table.Row>
          {kolonner.map((kolonne) => (
            <Table.ColumnHeader scope="col" sortable sortKey={kolonne.key} key={kolonne.key}>
              {kolonne.label}
            </Table.ColumnHeader>
          ))}
          <Table.ColumnHeader scope="col">Handlinger</Table.ColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>{children}</Table.Body>
    </Table>
  );
}
