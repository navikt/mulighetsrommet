import { DataDrivenTableDto } from "@tiltaksadministrasjon/api-client";
import { Heading, InfoCard } from "@navikt/ds-react";
import { useAktiveTilsagnTableData } from "@/pages/gjennomforing/tilsagn/detaljer/tilsagnDetaljerLoader";
import { DataDrivenTable } from "@mr/frontend-common";
import { InformationSquareIcon } from "@navikt/aksel-icons";

interface TilsagnTableProps {
  emptyStateMessage: string;
  data: DataDrivenTableDto;
}

export function TilsagnTable({ data, emptyStateMessage }: TilsagnTableProps) {
  return data.rows.length > 0 ? (
    <DataDrivenTable data={data} />
  ) : (
    <InfoCard data-color="info" className="mt-4">
      <InfoCard.Message icon={<InformationSquareIcon aria-hidden />}>
        {emptyStateMessage}
      </InfoCard.Message>
    </InfoCard>
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
        data={data.table}
      />
    </>
  );
}
