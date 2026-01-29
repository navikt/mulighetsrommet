import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { BodyShort, Link, Table } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";
import { ArrangorflateTilsagnRadDto } from "api-client/types.gen";
import { pathTo } from "~/utils/navigation";
import { TilsagnStatusTag } from "../tilsagn/TilsagnStatusTag";

export const tilsagnKolonner: Array<{ key: string; label: string }> = [
  { key: "tiltakNavn", label: "Tiltak" },
  { key: "arrangorNavn", label: "Arrangør" },
  { key: "periode", label: "Periode" },
  { key: "tilsagnNavn", label: "Tilsagn" },
  { key: "status", label: "Status" },
];

export function TilsagnRow({ row }: { row: ArrangorflateTilsagnRadDto }) {
  return (
    <Table.Row>
      <Table.HeaderCell>
        <strong>{row.tiltakTypeNavn}</strong>
        <BodyShort>{row.tiltakNavn}</BodyShort>
      </Table.HeaderCell>

      <Table.DataCell>{row.arrangorNavn}</Table.DataCell>

      <Table.DataCell>{formaterPeriode(row.periode)}</Table.DataCell>
      <Table.DataCell>{row.tilsagnNavn}</Table.DataCell>
      <Table.DataCell>
        <TilsagnStatusTag status={row.status} />
      </Table.DataCell>
      <Table.DataCell>
        <Link
          as={ReactRouterLink}
          aria-label={`Se detaljer om ${row.tilsagnNavn}`}
          to={pathTo.tilsagn(row.organisasjonsnummer, row.id)}
        >
          Se detaljer
        </Link>
      </Table.DataCell>
    </Table.Row>
  );
}
