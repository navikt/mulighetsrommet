import { TabellWrapper } from "@/components/tabell/TabellWrapper";
import { Alert, BodyShort, Box, Table } from "@navikt/ds-react";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { useGetInnsendinger } from "@/api/utbetaling/useFiltrerteInnsendinger";
import { InnsendingFilterStateAtom } from "./filter";
import { useSavedFiltersState } from "@/filter/useSavedFiltersState";
import { LagretFilterType } from "@tiltaksadministrasjon/api-client";
import { Link } from "react-router";
import { UtbetalingStatusTag } from "@/components/utbetaling/UtbetalingStatusTag";

interface Props {
  skjulKolonner?: Partial<Record<Kolonne, boolean>>;
}

export function InnsendingTable({ skjulKolonner }: Props) {
  const { filter } = useSavedFiltersState(InnsendingFilterStateAtom, LagretFilterType.INNSENDING);
  const { data: innsendinger } = useGetInnsendinger(filter.values);

  return (
    <Box paddingInline="2">
      <TabellWrapper>
        {innsendinger.length === 0 ? (
          <Alert variant="info">Fant ingen innsendinger</Alert>
        ) : (
          <Table data-testid="innsending-tabell">
            <Table.Header>
              <Table.Row>
                {headers
                  .filter((header) => {
                    return skjulKolonner ? !skjulKolonner[header.sortKey] : true;
                  })
                  .map((header) => (
                    <Table.ColumnHeader
                      key={header.sortKey}
                      sortKey={header.sortKey}
                      sortable={header.sortable}
                      style={{
                        width: header.width,
                      }}
                    >
                      {header.tittel}
                    </Table.ColumnHeader>
                  ))}
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {innsendinger.map((innsending, index) => {
                const formattertSluttDato = innsending.periode.slutt
                  ? formaterDato(innsending.periode.slutt)
                  : "-";
                const formattertStartDato = formaterDato(innsending.periode.start);

                return (
                  <Table.Row key={index}>
                    <Table.DataCell aria-label={`Enhet: ${innsending.kostnadssteder}`}>
                      <BodyShort size="small">
                        {innsending.kostnadssteder.map((sted) => sted.navn).join(", ")}
                      </BodyShort>
                    </Table.DataCell>
                    <Table.DataCell aria-label={`tiltakstype: ${innsending.tiltakstype}`}>
                      {innsending.tiltakstype.navn}
                    </Table.DataCell>
                    <Table.DataCell aria-label={`Virksomhetsnavn: ${innsending.arrangor}`}>
                      <BodyShort size="small">{innsending.arrangor}</BodyShort>
                    </Table.DataCell>
                    <Table.DataCell
                      title={`Startdato ${formattertStartDato}`}
                      aria-label={`Startdato: ${formattertStartDato}`}
                    >
                      {formattertStartDato}
                    </Table.DataCell>
                    <Table.DataCell
                      title={`Sluttdato ${formattertSluttDato}`}
                      aria-label={`Sluttdato ${formattertSluttDato}`}
                    >
                      {formattertSluttDato}
                    </Table.DataCell>
                    <Table.DataCell aria-label={`Beløp: ${innsending.belop}`}>
                      {innsending.belop ?? "-"}
                    </Table.DataCell>
                    <Table.DataCell aria-label={`Status: ${innsending.status}`}>
                      <UtbetalingStatusTag status={innsending.status} />
                    </Table.DataCell>
                    <Table.DataCell aria-label={`Beløp: ${innsending.belop}`}>
                      <Link
                        to={`/gjennomforinger/${innsending.gjennomforingId}/utbetalinger/${innsending.id}`}
                        replace
                      >
                        Detaljer
                      </Link>
                    </Table.DataCell>
                  </Table.Row>
                );
              })}
            </Table.Body>
          </Table>
        )}
      </TabellWrapper>
    </Box>
  );
}

interface ColumnHeader {
  sortKey: Kolonne;
  tittel: string;
  sortable: boolean;
  width: string;
}

const headers: ColumnHeader[] = [
  {
    sortKey: "enhet",
    tittel: "Enhet",
    sortable: false,
    width: "2fr",
  },
  {
    sortKey: "tiltakstype",
    tittel: "Tiltakstype",
    sortable: false,
    width: "2fr",
  },
  {
    sortKey: "arrangor",
    tittel: "Arrangør",
    sortable: true,
    width: "3fr",
  },
  {
    sortKey: "startdato",
    tittel: "Startdato",
    sortable: true,
    width: "1fr",
  },
  {
    sortKey: "sluttdato",
    tittel: "Sluttdato",
    sortable: true,
    width: "1fr",
  },
  {
    sortKey: "belop",
    tittel: "Beløp",
    sortable: false,
    width: "2fr",
  },
  {
    sortKey: "status",
    tittel: "Status",
    sortable: true,
    width: "2fr",
  },
  {
    sortKey: "lenke",
    tittel: "",
    sortable: false,
    width: "2fr",
  },
];

type Kolonne =
  | "belop"
  | "enhet"
  | "tiltakstype"
  | "arrangor"
  | "startdato"
  | "sluttdato"
  | "status"
  | "lenke";
