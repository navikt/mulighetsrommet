import { TabellWrapper } from "@/components/tabell/TabellWrapper";
import { Alert, BodyShort, Table, VStack } from "@navikt/ds-react";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { useGetInnsendinger } from "@/api/utbetaling/useFiltrerteInnsendinger";
import { InnsendingFilterStateAtom, InnsendingFilterType } from "./filter";
import { useSavedFiltersState } from "@/filter/useSavedFiltersState";
import { LagretFilterType } from "@tiltaksadministrasjon/api-client";
import { Link } from "react-router";
import { UtbetalingStatusTag } from "@/components/utbetaling/UtbetalingStatusTag";

interface Props {
  skjulKolonner?: Partial<Record<Kolonne, boolean>>;
  updateFilter: (values: Partial<InnsendingFilterType>) => void;
}

export function InnsendingTable({ skjulKolonner, updateFilter }: Props) {
  const { filter } = useSavedFiltersState(InnsendingFilterStateAtom, LagretFilterType.INNSENDING);
  const { data: innsendinger } = useGetInnsendinger(filter.values);

  const sort = filter.values.sortering.tableSort;

  const handleSort = (sortKey: string) => {
    // Hvis man bytter sortKey starter vi med ascending
    const direction =
      sort.orderBy === sortKey
        ? sort.direction === "descending"
          ? "ascending"
          : "descending"
        : "ascending";
    updateFilter({
      sortering: {
        sortString: `${sortKey}-${direction}`,
        tableSort: { orderBy: sortKey, direction },
      },
    });
  };

  return (
    <VStack gap="6" paddingInline="2">
      <Alert variant="info">
        Under ser du en oversikt over manglende innsendinger for tiltak med fast sats, avtalt
        månedspris eller avtalt ukespris. Tiltak med avtalt timespris eller annen avtalt pris
        mangler foreløpig i oversikten
      </Alert>
      <TabellWrapper>
        {innsendinger.length === 0 ? (
          <Alert variant="info">Fant ingen innsendinger</Alert>
        ) : (
          <Table
            data-testid="innsending-tabell"
            sort={sort}
            onSortChange={(sortKey) => handleSort(sortKey)}
          >
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
              {innsendinger.map((innsending, index) => (
                <Table.Row key={index}>
                  <Table.DataCell aria-label={`Virksomhetsnavn: ${innsending.arrangor}`}>
                    <BodyShort size="small">{innsending.arrangor}</BodyShort>
                  </Table.DataCell>
                  <Table.DataCell aria-label={`Tiltakstype: ${innsending.tiltakstype}`}>
                    {innsending.tiltakstype.navn}
                  </Table.DataCell>
                  <Table.DataCell
                    aria-label={`Kostnadssteder: ${innsending.kostnadssteder.map((sted) => sted.navn).join(", ")}`}
                  >
                    <BodyShort size="small">
                      {innsending.kostnadssteder.length > 0
                        ? innsending.kostnadssteder.map((k) => k.navn).join(", ")
                        : "-"}
                    </BodyShort>
                  </Table.DataCell>
                  <Table.DataCell
                    title={`periode ${formaterPeriode(innsending.periode)}`}
                    aria-label={`periode: ${formaterPeriode(innsending.periode)}`}
                  >
                    <BodyShort>{formaterPeriode(innsending.periode)} </BodyShort>
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
                    >
                      Detaljer
                    </Link>
                  </Table.DataCell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table>
        )}
      </TabellWrapper>
    </VStack>
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
    sortKey: "arrangor",
    tittel: "Arrangør",
    sortable: true,
    width: "3fr",
  },
  {
    sortKey: "tiltakstype",
    tittel: "Tiltakstype",
    sortable: true,
    width: "2fr",
  },
  {
    sortKey: "kostnadssted",
    tittel: "Kostnadssted",
    sortable: false,
    width: "1fr",
  },
  {
    sortKey: "periode",
    tittel: "Periode",
    sortable: true,
    width: "3fr",
  },
  {
    sortKey: "belop",
    tittel: "Beløp",
    sortable: false,
    width: "1fr",
  },
  {
    sortKey: "status",
    tittel: "Status",
    sortable: false,
    width: "2fr",
  },
  {
    sortKey: "lenke",
    tittel: "",
    sortable: false,
    width: "1fr",
  },
];

type Kolonne =
  | "belop"
  | "kostnadssted"
  | "tiltakstype"
  | "arrangor"
  | "periode"
  | "status"
  | "lenke";
