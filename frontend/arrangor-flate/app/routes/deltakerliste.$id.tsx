import { Alert, Button, HGrid, SortState, Table } from "@navikt/ds-react";
import type { LoaderFunction, MetaFunction } from "@remix-run/node";
import { Link, useLoaderData } from "@remix-run/react";
import { PageHeader } from "../components/PageHeader";
import { DeltakerlisteDetaljer } from "../components/deltakerliste/DeltakerlisteDetaljer";
import { Deltaker, Deltakerliste } from "../domene/domene";
import { requirePersonIdent } from "../auth/auth.server";
import { RefusjonskravService } from "@mr/api-client";
import { useState } from "react";

export const meta: MetaFunction = () => {
  return [{ title: "Refusjon" }, { name: "description", content: "Refusjonsdetaljer" }];
};

type LoaderData = {
  deltakerliste: Deltakerliste;
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  await requirePersonIdent(request);
  if (params.id === undefined) throw Error("Mangler id");

  const krav = await RefusjonskravService.getRefusjonkrav({
    id: params.id,
  });

  return {
    deltakerliste: {
      id: params.id,
      detaljer: {
        tiltaksnavn: krav?.tiltaksgjennomforing?.navn,
        tiltaksnummer: "2024/123456",
        avtalenavn: "AFT - Fredrikstad, Sarpsborg, Halden",
        tiltakstype: "Arbeidsforberedende trening",
        refusjonskravperiode: `${krav.periodeStart} - ${krav.periodeSlutt}`,
        refusjonskravnummer: "6",
      },
      deltakere: [
        {
          navn: "Skånsom seigmann",
          veileder: "Viggo Veileder",
          fodselsdato: "01.01.2001",
          startDatoTiltaket: "01.01.2024",
          startDatoPerioden: "01.01.2024",
          sluttDatoPerioden: "31.01.2024",
          deltakelsesProsent: 100,
          maanedsverk: 1,
          belop: 20205,
        },
        {
          navn: "Oksydert Fjellkjede",
          veileder: "Viggo Veileder",
          fodselsdato: "27467202907",
          startDatoTiltaket: "01.02.2024",
          startDatoPerioden: "01.02.2024",
          sluttDatoPerioden: "31.04.2024",
          deltakelsesProsent: 100,
          maanedsverk: 0.5,
          belop: 10103,
        },
      ],
    },
  };
};

interface ScopedSortState extends SortState {
  orderBy: Deltaker["navn"];
}

export default function RefusjonDeltakerlister() {
  const { deltakerliste } = useLoaderData<LoaderData>();
  const [sort, setSort] = useState<ScopedSortState | undefined>();

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

  const sortedData = deltakerliste.deltakere.slice().sort((a, b) => {
    if (sort) {
      return sort.direction === "ascending"
        ? comparator(b, a, sort.orderBy)
        : comparator(a, b, sort.orderBy);
    }
    return 1;
  });

  console.log(deltakerliste);

  return (
    <>
      <PageHeader
        title="Deltakerliste"
        tilbakeLenke={{ navn: "Tilbake til refusjonsliste", url: "/" }}
      />
      <HGrid gap="5" columns={1}>
        <DeltakerlisteDetaljer deltakerliste={deltakerliste} />
        <div className="flex justify-between mt-8">
          <span>
            Refusjonskravperiode: <b>{deltakerliste.detaljer.refusjonskravperiode}</b>
          </span>
          <span>
            Refusjonskravnummer: <b>{deltakerliste.detaljer.refusjonskravnummer}</b>
          </span>
        </div>
        <Alert variant="info">Her kommer deltakertabell</Alert>
        <Table
          sort={sort}
          onSortChange={(sortKey) => handleSort(sortKey as ScopedSortState["orderBy"])}
          zebraStripes
        >
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader scope="col" sortable sortKey="name">
                Etternavn, Fornavn
              </Table.ColumnHeader>
              <Table.ColumnHeader scope="col" sortable sortKey="veileder">
                Veileder
              </Table.ColumnHeader>
              <Table.HeaderCell scope="col">Fødselsdato</Table.HeaderCell>
              <Table.HeaderCell scope="col">Startdato i tiltaket</Table.HeaderCell>
              <Table.ColumnHeader scope="col" sortable sortKey="startDatePeriod">
                Startdato i perioden
              </Table.ColumnHeader>
              <Table.ColumnHeader scope="col" sortable sortKey="endDatePeriod">
                Sluttdato i perioden
              </Table.ColumnHeader>
              <Table.HeaderCell scope="col">Deltakelses-prosent</Table.HeaderCell>
              <Table.HeaderCell scope="col">Månedsverk i perioden</Table.HeaderCell>
              <Table.HeaderCell scope="col">Beregnet beløp</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {sortedData.map((deltaker, i) => {
              return (
                <Table.Row key={i + deltaker.fodselsdato}>
                  <Table.HeaderCell>{deltaker.navn}</Table.HeaderCell>
                  <Table.DataCell>{deltaker.veileder}</Table.DataCell>
                  <Table.DataCell>{deltaker.fodselsdato}</Table.DataCell>
                  <Table.DataCell>{deltaker.startDatoTiltaket}</Table.DataCell>
                  <Table.DataCell>{deltaker.startDatoPerioden}</Table.DataCell>
                  <Table.DataCell>{deltaker.sluttDatoPerioden}</Table.DataCell>
                  <Table.DataCell>{deltaker.deltakelsesProsent}</Table.DataCell>
                  <Table.DataCell>{deltaker.maanedsverk}</Table.DataCell>
                  <Table.DataCell>{deltaker.belop}</Table.DataCell>
                </Table.Row>
              );
            })}
          </Table.Body>
        </Table>
        <Button
          as={Link}
          className="justify-self-end"
          to={`/deltakerliste/detaljer/${deltakerliste.id}`}
        >
          Neste
        </Button>
      </HGrid>
    </>
  );
}
