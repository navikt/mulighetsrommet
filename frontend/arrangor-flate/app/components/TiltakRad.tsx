import { TabelloversiktRadDto } from "@api-client";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { BodyShort, Link, Table } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";
import { pathTo } from "~/utils/navigation";

export function TiltakRad({ row }: { row: TabelloversiktRadDto }) {
  return (
    <Table.Row>
      <Table.HeaderCell>
        <strong>{row.tiltakstypeNavn}</strong>
        <BodyShort>
          {row.tiltakNavn} ({row.lopenummer})
        </BodyShort>
      </Table.HeaderCell>

      <Table.DataCell>
        {row.arrangorNavn} ({row.organisasjonsnummer})
      </Table.DataCell>
      <Table.DataCell>{formaterDato(row.startDato)}</Table.DataCell>
      <Table.DataCell>{formaterDato(row.sluttDato)}</Table.DataCell>
      <Table.DataCell>
        <Link
          as={ReactRouterLink}
          to={pathTo.innsendingsinformasjon(row.organisasjonsnummer, row.gjennomforingId)}
        >
          Start innsending
        </Link>
      </Table.DataCell>
    </Table.Row>
  );
}
