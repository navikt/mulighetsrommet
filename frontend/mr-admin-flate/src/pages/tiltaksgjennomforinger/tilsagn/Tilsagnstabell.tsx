import { formaterDato } from "@/utils/Utils";
import { TilsagnBesluttelse, TilsagnDto } from "@mr/api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { HelpText, HStack, SortState, Table, Tag } from "@navikt/ds-react";
import { TableColumnHeader } from "@navikt/ds-react/Table";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";

interface Props {
  tilsagn: TilsagnDto[];
}

interface ScopedSortState extends SortState {
  orderBy: keyof TilsagnDto;
}

export function Tilsagnstabell({ tilsagn }: Props) {
  const { tiltaksgjennomforingId } = useParams();

  const [sort, setSort] = useState<ScopedSortState>();

  const handleSort = (sortKey: ScopedSortState["orderBy"]) => {
    setSort(
      sort && sortKey === sort.orderBy && sort.direction === "descending"
        ? undefined
        : {
            orderBy: sortKey,
            direction:
              sort && sortKey === sort.orderBy && sort.direction === "ascending"
                ? "descending"
                : "ascending",
          },
    );
  };

  function comparator<T>(a: T, b: T, orderBy: keyof T): number {
    if (b[orderBy] == null || b[orderBy] < a[orderBy]) {
      return -1;
    }
    if (b[orderBy] > a[orderBy]) {
      return 1;
    }
    return 0;
  }

  function TilsagnStatus(props: { tilsagn: TilsagnDto }) {
    const { tilsagn } = props;

    if (tilsagn.besluttelse) {
      return (
        <>
          <Tag
            variant={
              tilsagn.besluttelse.utfall === TilsagnBesluttelse.GODKJENT ? "success" : "warning"
            }
          >
            <HStack justify={"space-between"} gap="2" align={"center"}>
              {besluttelseTilTekst(tilsagn.besluttelse.utfall)}{" "}
              <HelpText>
                {besluttelseTilTekst(tilsagn.besluttelse.utfall)} den{" "}
                {formaterDato(tilsagn.besluttelse.tidspunkt)} av {tilsagn.besluttelse.navIdent}
              </HelpText>
            </HStack>
          </Tag>
        </>
      );
    } else if (tilsagn.annullertTidspunkt) {
      return <Tag variant="neutral"> Annullert</Tag>;
    } else {
      return <Tag variant="info">Til beslutning</Tag>;
    }
  }

  const sortedData = [...tilsagn].sort((a, b) => {
    if (sort) {
      return sort.direction === "ascending"
        ? comparator(b, a, sort.orderBy)
        : comparator(a, b, sort.orderBy);
    }
    return 1;
  });

  return (
    <Table
      sort={sort}
      onSortChange={(sortKey) => handleSort(sortKey as ScopedSortState["orderBy"])}
    >
      <Table.Header>
        <Table.Row>
          <TableColumnHeader sortKey="periodeStart" sortable>
            Periodestart
          </TableColumnHeader>
          <TableColumnHeader sortKey="periodeSlutt" sortable>
            Periodeslutt
          </TableColumnHeader>
          <TableColumnHeader sortKey="kostnadssted.navn" sortable>
            Kostnadssted
          </TableColumnHeader>
          <TableColumnHeader sortKey="tiltaksgjennomforing.antallPlasser" sortable>
            Antall plasser
          </TableColumnHeader>
          <TableColumnHeader sortKey="beregning.belop" sortable>
            Beløp
          </TableColumnHeader>
          <TableColumnHeader>Status</TableColumnHeader>
          <TableColumnHeader></TableColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map((tilsagn) => {
          const { periodeStart, periodeSlutt, kostnadssted, beregning, id, tiltaksgjennomforing } =
            tilsagn;
          return (
            <Table.Row key={id}>
              <Table.DataCell>{formaterDato(periodeStart)}</Table.DataCell>
              <Table.DataCell>{formaterDato(periodeSlutt)}</Table.DataCell>
              <Table.DataCell>{kostnadssted.navn}</Table.DataCell>
              <Table.DataCell>{tiltaksgjennomforing.antallPlasser}</Table.DataCell>
              <Table.DataCell>{formaterNOK(beregning.belop)}</Table.DataCell>
              <Table.DataCell>
                <TilsagnStatus tilsagn={tilsagn} />
              </Table.DataCell>

              <Table.DataCell>
                <Link to={`/tiltaksgjennomforinger/${tiltaksgjennomforingId}/tilsagn/${id}`}>
                  Detaljer
                </Link>
              </Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}

function besluttelseTilTekst(besluttelse: TilsagnBesluttelse): "Godkjent" | "Avvist" {
  return besluttelse === "GODKJENT" ? "Godkjent" : "Avvist";
}
