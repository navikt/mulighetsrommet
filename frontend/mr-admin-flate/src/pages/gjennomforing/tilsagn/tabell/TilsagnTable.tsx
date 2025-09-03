import { DataDrivenTableDto } from "@tiltaksadministrasjon/api-client";
import { Alert, Heading } from "@navikt/ds-react";
import { DataDrivenTable } from "@/components/tabell/DataDrivenTable";
import { useAktiveTilsagnTableData } from "@/pages/gjennomforing/tilsagn/detaljer/tilsagnDetaljerLoader";

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
  gjennomforingId: string;
}

export function AktiveTilsagnTable({ gjennomforingId }: AktiveTilsagnTableProps) {
  const { data } = useAktiveTilsagnTableData(gjennomforingId);
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
