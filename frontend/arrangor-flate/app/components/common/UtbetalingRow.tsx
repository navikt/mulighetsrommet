import { TabelloversiktRadDto } from "@api-client";
import { UtbetalingTypeTag } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { BodyShort, Table } from "@navikt/ds-react";
import { UtbetalingStatusTag } from "../utbetaling/UtbetalingStatusTag";
import { UtbetalingTextLink } from "../utbetaling/UtbetalingTextLink";

export function UtbetalingRow({ row, periode }: { row: TabelloversiktRadDto; periode?: boolean }) {
  const formatertPeriode = periode ? (
    <Table.DataCell>
      {row.sluttDato && formaterPeriode({ start: row.startDato, slutt: row.sluttDato })}
    </Table.DataCell>
  ) : (
    <>
      <Table.DataCell>{formaterDato(row.startDato)}</Table.DataCell>
      <Table.DataCell>{formaterDato(row.sluttDato)}</Table.DataCell>
    </>
  );

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

      {formatertPeriode}

      {row.belop && <Table.DataCell>{row.belop}</Table.DataCell>}
      {row.type && (
        <Table.DataCell>
          <UtbetalingTypeTag type={row.type} />
        </Table.DataCell>
      )}
      {row.status && (
        <Table.DataCell>
          <UtbetalingStatusTag status={row.status} />
        </Table.DataCell>
      )}

      <Table.DataCell>
        <UtbetalingTextLink
          status={row.status ?? undefined}
          gjennomforingNavn={row.tiltakNavn}
          utbetalingId={row.gjennomforingId}
          orgnr={row.organisasjonsnummer}
        />
      </Table.DataCell>
    </Table.Row>
  );
}
