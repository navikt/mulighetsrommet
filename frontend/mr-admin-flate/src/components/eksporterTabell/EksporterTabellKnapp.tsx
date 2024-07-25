import { Button } from "@navikt/ds-react";
import { FileExcelIcon } from "@navikt/aksel-icons";

interface Props {
  lastNedExcel: () => void;
  lasterExcel: boolean;
}

export function EksporterTabellKnapp({ lastNedExcel, lasterExcel }: Props) {
  return (
    <Button
      icon={<FileExcelIcon title="Excelikon" />}
      variant="tertiary"
      onClick={lastNedExcel}
      disabled={lasterExcel}
      type="button"
    >
      {lasterExcel ? "Henter Excel-fil..." : "Eksporter tabellen til Excel"}
    </Button>
  );
}
