import { Alert, Button, HGrid, Table } from "@navikt/ds-react";
import type { LoaderFunction, MetaFunction } from "@remix-run/node";
import { Link, useLoaderData } from "@remix-run/react";
import { PageHeader } from "../components/PageHeader";
import { DeltakerlisteDetaljer } from "../components/deltakerliste/DeltakerlisteDetaljer";
import { Deltakerliste } from "../domene/domene";
import { requirePersonIdent } from "../auth/auth.server";
import { RefusjonskravService } from "@mr/api-client";

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
        refusjonskravperiode: "01.01.2024 - 31.01.2024",
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

export default function RefusjonDeltakerlister() {
  const { deltakerliste } = useLoaderData<LoaderData>();
  return (
    <>
      <PageHeader
        title="Deltakerliste"
        tilbakeLenke={{ navn: "Tilbake til refusjonsliste", url: "/" }}
      />
      <HGrid gap="5" columns={1}>
        <DeltakerlisteDetaljer deltakerliste={deltakerliste} />
        <Alert variant="info">Her kommer deltakertabell</Alert>
        <Table zebraStripes>
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
            {deltakerliste.deltakere.map((deltaker, i) => {
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
