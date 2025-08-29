import { DataDrivenTableDto } from "@mr/api-client-v2";
import { Alert, Heading } from "@navikt/ds-react";
import { DataDrivenTable } from "@/components/tabell/DataDrivenTable";

interface TilsagnTableProps {
  emptyStateMessage: string;
  data: DataDrivenTableDto;
}

export function TilsagnTable({ data, emptyStateMessage }: TilsagnTableProps) {
  return data.rows.length > 0 ? (
    <DataDrivenTable data={data} />
  ) : (
    <Alert variant="info" className="mt-4">
      {emptyStateMessage}
    </Alert>
  );
}

interface AktiveTilsagnTableProps {
  data: DataDrivenTableDto;
}

export function AktiveTilsagnTable({ data }: AktiveTilsagnTableProps) {
  return (
    <>
      <Heading size="medium">Aktive tilsagn</Heading>
      <TilsagnTable
        emptyStateMessage="Det finnes ingen aktive tilsagn for dette tiltaket"
        data={data}
      />
    </>
  );
}
