import { UtbetalingTypeTag } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import { formaterPeriodeUdefinertSlutt } from "@mr/frontend-common/utils/date";
import { BodyShort, Link, Table } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";
import { UtbetalingStatusTag } from "../utbetaling/UtbetalingStatusTag";
import { UtbetalingTextLink } from "../utbetaling/UtbetalingTextLink";
import { ArrangorInnsendingRadDto } from "api-client/types.gen";
import { pathTo } from "~/utils/navigation";

export const utbetalingKolonner: Array<{ key: string; label: string }> = [
  { key: "tiltakNavn", label: "Tiltak" },
  { key: "arrangorNavn", label: "Arrangør" },
  { key: "startDato", label: "Periode" },
  { key: "belop", label: "Beløp" },
  { key: "type", label: "Type" },
  { key: "status", label: "Status" },
];

export function UtbetalingRow({ row }: { row: ArrangorInnsendingRadDto }) {
  return (
    <Table.Row>
      <Table.HeaderCell scope="row">
        <strong>{row.tiltakstypeNavn}</strong>
        <BodyShort>
          {row.tiltakNavn} ({row.lopenummer})
        </BodyShort>
      </Table.HeaderCell>

      <Table.DataCell>
        {row.arrangorNavn} ({row.organisasjonsnummer})
      </Table.DataCell>

      <Table.DataCell>
        {formaterPeriodeUdefinertSlutt({ start: row.startDato, slutt: row.sluttDato })}
      </Table.DataCell>

      {row.belop ? (
        <Table.DataCell
          align="right"
          className="whitespace-nowrap"
        >{`${row.belop.belop} ${row.belop.valuta}`}</Table.DataCell>
      ) : null}
      {row.type ? (
        <Table.DataCell>
          <UtbetalingTypeTag type={row.type} />
        </Table.DataCell>
      ) : null}
      {row.status ? (
        <Table.DataCell>
          <UtbetalingStatusTag status={row.status} />
        </Table.DataCell>
      ) : null}

      <Table.DataCell>
        {row.status && row.utbetalingId ? (
          <UtbetalingTextLink
            status={row.status}
            gjennomforingNavn={row.tiltakNavn}
            utbetalingId={row.utbetalingId}
            orgnr={row.organisasjonsnummer}
          />
        ) : (
          <Link
            as={ReactRouterLink}
            aria-label={`Start innsending for krav om utbetaling for ${row.tiltakNavn}`}
            to={pathTo.opprettKrav(row.organisasjonsnummer, row.gjennomforingId)}
          >
            Start innsending
          </Link>
        )}
      </Table.DataCell>
    </Table.Row>
  );
}
