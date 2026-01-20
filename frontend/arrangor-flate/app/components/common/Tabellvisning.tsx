import { SortState, Table } from "@navikt/ds-react";
interface TabellvisningProps extends React.PropsWithChildren {
  kolonner: Array<{ key: string; label: string }>;
  sort: SortState | undefined;
  onSortChange: (key: string) => void;
}

export function Tabellvisning({ kolonner, children, sort, onSortChange }: TabellvisningProps) {
  return (
    <Table sort={sort} onSortChange={onSortChange}>
      <Table.Header>
        <Table.Row>
          {kolonner.map((kolonne) => (
            <Table.ColumnHeader sortable sortKey={kolonne.key} key={kolonne.key}>
              {kolonne.label}
            </Table.ColumnHeader>
          ))}
          <Table.ColumnHeader>Handlinger</Table.ColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>{children}</Table.Body>
    </Table>
  );
}
