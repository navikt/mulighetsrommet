import { TiltakDeltMedBrukerDto } from "@api-client";
import { BodyShort, Box, Button, HStack, List, Table, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import { Link } from "react-router";
import { VisningsnavnForTiltak } from "@/components/oversikt/VisningsnavnForTiltak";
import { formaterDato } from "@/utils/Utils";
import { ModiaRoute, navigateToModiaApp } from "../ModiaRoute";
import { useDeltMedBrukerHistorikk } from "../hooks/useDeltMedBrukerHistorikk";
import { IngenFunnetBox } from "../views/Landingsside";

function sortOnCreatedAt(a: TiltakDeltMedBrukerDto, b: TiltakDeltMedBrukerDto) {
  return new Date(b.deling.tidspunkt).getTime() - new Date(a.deling.tidspunkt).getTime();
}

export function DelMedBrukerHistorikk() {
  const { data = [] } = useDeltMedBrukerHistorikk();

  if (data.length === 0) {
    return <IngenFunnetBox title="Det er ikke delt informasjon om noen tiltak med brukeren" />;
  }

  const gruppertHistorikk = data.sort(sortOnCreatedAt).reduce(
    (acc, obj) => {
      // If the tiltakId key doesn't exist, create it with an empty array
      if (!acc[obj.tiltak.id]) {
        acc[obj.tiltak.id] = [];
      }
      // Push the current object to the array for its tiltakId
      acc[obj.tiltak.id]!.push(obj);
      return acc;
    },
    {} as Record<string, TiltakDeltMedBrukerDto[] | undefined>,
  ) as Record<string, TiltakDeltMedBrukerDto[]>;

  // Sort each group by createdAt
  Object.keys(gruppertHistorikk).forEach((tiltakId) => {
    gruppertHistorikk[tiltakId].sort(sortOnCreatedAt);
  });

  return (
    <Box padding="2" background="bg-default">
      <VStack gap="2">
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell />
              <Table.HeaderCell scope="col">Tiltak</Table.HeaderCell>
              <Table.HeaderCell scope="col">Delt</Table.HeaderCell>
              <Table.HeaderCell scope="col"></Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {Object.keys(gruppertHistorikk).map((tiltakId) => {
              const delteTiltak = gruppertHistorikk[tiltakId];
              const sisteDelt = delteTiltak[0];

              if (delteTiltak.length === 1) {
                return (
                  <Table.Row key={tiltakId}>{createCells(delteTiltak.length, sisteDelt)}</Table.Row>
                );
              } else {
                return (
                  <Table.ExpandableRow
                    expandOnRowClick
                    key={tiltakId}
                    content={contentForRow(delteTiltak)}
                  >
                    {createCells(delteTiltak.length, sisteDelt)}
                  </Table.ExpandableRow>
                );
              }
            })}
          </Table.Body>
        </Table>
      </VStack>
    </Box>
  );
}

function contentForRow(delinger: TiltakDeltMedBrukerDto[]): ReactNode {
  const tidligereDelinger = delinger.slice(1);

  return (
    <List title="Tidligere delinger">
      {tidligereDelinger.map(({ deling, tiltak, tiltakstype }) => {
        return (
          <List.Item key={deling.dialogId}>
            <HStack gap="5" align="start">
              <VisningsnavnForTiltak noLink tiltakstypeNavn={tiltakstype.navn} navn={tiltak.navn} />
              <BodyShort size="small">Delt {formaterDato(deling.tidspunkt)}</BodyShort>
            </HStack>
          </List.Item>
        );
      })}
    </List>
  );
}

function createCells(antallTiltakDelt: number, deltMedBruker: TiltakDeltMedBrukerDto): ReactNode {
  return (
    <>
      {antallTiltakDelt === 1 ? <Table.DataCell></Table.DataCell> : null}
      <Table.DataCell>
        <VisningsnavnForTiltak
          noLink
          tiltakstypeNavn={deltMedBruker.tiltakstype.navn}
          navn={deltMedBruker.tiltak.navn}
        />
      </Table.DataCell>
      <Table.DataCell>{formaterDato(deltMedBruker.deling.tidspunkt)}</Table.DataCell>
      <Table.DataCell>
        <VStack align="center" gap="2">
          <Button
            variant="secondary"
            size="small"
            onClick={(e) => {
              e.preventDefault();
              navigateToModiaApp({
                route: ModiaRoute.DIALOG,
                dialogId: deltMedBruker.deling.dialogId,
              });
            }}
          >
            Gå til dialogen
          </Button>
          <Link
            to={`/arbeidsmarkedstiltak/tiltak/${deltMedBruker.tiltak.id}`}
            className="text-center text-base no-underline hover:underline"
          >
            Gå til tiltak
          </Link>
        </VStack>
      </Table.DataCell>
    </>
  );
}
