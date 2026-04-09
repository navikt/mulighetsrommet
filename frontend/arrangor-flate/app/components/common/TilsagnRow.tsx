import { formaterPeriodeSlutt, formaterPeriodeStart } from "@mr/frontend-common/utils/date";
import { BodyShort, Link, Table } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";
import { ArrangorflateTilsagnRadDto } from "api-client/types.gen";
import { pathTo } from "~/utils/navigation";
import { TilsagnStatusTag } from "../tilsagn/TilsagnStatusTag";
import { Kolonne } from "./Tabellvisning";

export const tilsagnKolonner: Array<Kolonne> = [
  { key: "tiltakNavn", label: "Tiltak", sortable: true },
  { key: "arrangorNavn", label: "Arrangør", sortable: true },
  { key: "startDato", label: "Periode start", sortable: true },
  { key: "sluttDato", label: "Periode slutt", sortable: true },
  { key: "tilsagnNavn", label: "Tilsagn", sortable: true },
  { key: "status", label: "Status", sortable: true },
];

export function TilsagnRow({ row }: { row: ArrangorflateTilsagnRadDto }) {
  return (
    <Table.Row>
      <Table.HeaderCell>
        <strong>{row.tiltakTypeNavn}</strong>
        <BodyShort>{row.tiltakNavn}</BodyShort>
      </Table.HeaderCell>

      <Table.DataCell>{row.arrangorNavn}</Table.DataCell>

      <Table.DataCell>{formaterPeriodeStart(row.periode)}</Table.DataCell>
      <Table.DataCell>{formaterPeriodeSlutt(row.periode)}</Table.DataCell>
      <Table.DataCell>
        <BodyShort>{row.tilsagnType}</BodyShort>
        <BodyShort>{row.bestillingsnummer}</BodyShort>
      </Table.DataCell>
      <Table.DataCell>
        <TilsagnStatusTag status={row.status} />
      </Table.DataCell>
      <Table.DataCell>
        <Link
          as={ReactRouterLink}
          aria-label={`Se detaljer om ${row.tilsagnType} (${row.bestillingsnummer})`}
          to={pathTo.tilsagn(row.organisasjonsnummer, row.id)}
        >
          Se detaljer
        </Link>
      </Table.DataCell>
    </Table.Row>
  );
}
