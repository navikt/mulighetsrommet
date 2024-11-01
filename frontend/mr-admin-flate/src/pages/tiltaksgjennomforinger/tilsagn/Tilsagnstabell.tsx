import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { formaterDato } from "@/utils/Utils";
import { NavAnsatt, NavAnsattRolle, TilsagnBesluttelse, TilsagnDto } from "@mr/api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { ClockIcon } from "@navikt/aksel-icons";
import { Alert, Button, HelpText, HStack, Table, SortState } from "@navikt/ds-react";
import { TableColumnHeader } from "@navikt/ds-react/Table";
import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

interface Props {
  tilsagn: TilsagnDto[];
}

interface ScopedSortState extends SortState {
  orderBy: keyof TilsagnDto;
}

export function Tilsagnstabell({ tilsagn }: Props) {
  const { tiltaksgjennomforingId } = useParams();
  const { data: ansatt } = useHentAnsatt();
  const navigate = useNavigate();

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

  function besluttTilsagn(id: string) {
    navigate(id);
  }

  function redigerTilsagn(id: string) {
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforingId}/tilsagn/${id}/rediger-tilsagn`);
  }

  function TilsagnStatus(props: { tilsagn: TilsagnDto; ansatt?: NavAnsatt }) {
    const { tilsagn, ansatt } = props;

    if (tilsagn.besluttelse) {
      return (
        <Alert
          inline
          size="small"
          variant={tilsagn.besluttelse.utfall === "GODKJENT" ? "success" : "warning"}
        >
          <HStack justify={"space-between"} gap="2" align={"center"}>
            {besluttelseTilTekst(tilsagn.besluttelse.utfall)}{" "}
            <HelpText>
              {besluttelseTilTekst(tilsagn.besluttelse.utfall)} den{" "}
              {formaterDato(tilsagn.besluttelse.tidspunkt)} av {tilsagn.besluttelse.navIdent}
            </HelpText>
          </HStack>
        </Alert>
      );
    } else if (tilsagn.annullertTidspunkt) {
      return (
        <HStack justify={"space-between"} gap="2" align={"center"}>
          Annullert
          <HelpText>{`Annullert den ${formaterDato(tilsagn.annullertTidspunkt)}`}</HelpText>
        </HStack>
      );
    } else if (
      ansatt?.roller.includes(NavAnsattRolle.OKONOMI_BESLUTTER) &&
      tilsagn.opprettetAv !== ansatt?.navIdent
    ) {
      return (
        <Button
          type="button"
          variant="primary"
          size="small"
          onClick={() => besluttTilsagn(tilsagn.id)}
        >
          Beslutt
        </Button>
      );
    } else {
      return (
        <span>
          <HStack align={"center"} gap="1">
            <ClockIcon /> Til beslutning
          </HStack>
        </span>
      );
    }
  }

  const sortedData = tilsagn.slice().sort((a, b) => {
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
          <Table.ColumnHeader title="Tilsagnsnummer">Tilsagnsnr.</Table.ColumnHeader>
          <TableColumnHeader sortKey="periodeStart" sortable>
            Periodestart
          </TableColumnHeader>
          <TableColumnHeader sortKey="periodeSlutt" sortable>
            Periodeslutt
          </TableColumnHeader>
          <TableColumnHeader>Kostnadssted</TableColumnHeader>
          <TableColumnHeader>Antall plasser</TableColumnHeader>
          <TableColumnHeader>Beløp</TableColumnHeader>
          <TableColumnHeader>Status</TableColumnHeader>
          <TableColumnHeader></TableColumnHeader>
          <TableColumnHeader></TableColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map((tilsagn) => {
          const {
            periodeStart,
            periodeSlutt,
            kostnadssted,
            beregning,
            id,
            besluttelse,
            tiltaksgjennomforing,
          } = tilsagn;
          return (
            <Table.Row key={id}>
              <Table.DataCell>
                {tiltaksgjennomforing.tiltaksnummer}/{tilsagn.lopenummer}
              </Table.DataCell>
              <Table.DataCell>{formaterDato(periodeStart)}</Table.DataCell>
              <Table.DataCell>{formaterDato(periodeSlutt)}</Table.DataCell>
              <Table.DataCell>{kostnadssted.navn}</Table.DataCell>
              <Table.DataCell>{tiltaksgjennomforing.antallPlasser}</Table.DataCell>
              <Table.DataCell>{formaterNOK(beregning.belop)}</Table.DataCell>
              <Table.DataCell>
                <TilsagnStatus tilsagn={tilsagn} ansatt={ansatt} />
              </Table.DataCell>
              <Table.DataCell>
                {tilsagn?.opprettetAv === ansatt?.navIdent &&
                besluttelse?.utfall === TilsagnBesluttelse.AVVIST ? (
                  <Button
                    type="button"
                    variant="primary"
                    size="small"
                    onClick={() => redigerTilsagn(id)}
                  >
                    Korriger
                  </Button>
                ) : null}
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
