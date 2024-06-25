import { Accordion, Alert, Button, Skeleton, Table, VStack } from "@navikt/ds-react";
import { TiltakDeltMedBruker } from "mulighetsrommet-api-client";
import { formaterDato } from "../../../utils/Utils";
import { ModiaRoute, navigateToModiaApp } from "../ModiaRoute";
import { useDeltMedBrukerHistorikk } from "../hooks/useDeltMedBrukerHistorikk";

function sortOnCreatedAt(a: TiltakDeltMedBruker, b: TiltakDeltMedBruker) {
  return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
}

export function DelMedBrukerHistorikk() {
  const { data = [], isLoading, error } = useDeltMedBrukerHistorikk();

  if (error) {
    return <Alert variant="error">Kunne ikke hente Delt med bruker-historikk</Alert>;
  }

  if (isLoading) {
    return (
      <VStack gap="2">
        <Skeleton width="100%" height="3rem" />
        <Skeleton width="100%" height="3rem" />
        <Skeleton width="100%" height="3rem" />
      </VStack>
    );
  }

  if (data.length === 0) {
    return <Alert variant="info">Det er ikke delt informasjon om noen tiltak med brukeren</Alert>;
  }

  const gruppertHistorikk = data.sort(sortOnCreatedAt).reduce(
    (acc, obj) => {
      // If the tiltakId key doesn't exist, create it with an empty array
      if (!acc[obj.tiltakId]) {
        acc[obj.tiltakId] = [];
      }
      // Push the current object to the array for its tiltakId
      acc[obj.tiltakId].push(obj);
      return acc;
    },
    {} as Record<string, TiltakDeltMedBruker[]>,
  );

  // Sort each group by createdAt
  Object.keys(gruppertHistorikk).forEach((tiltakId) => {
    gruppertHistorikk[tiltakId].sort(sortOnCreatedAt);
  });

  return (
    <VStack gap="5">
      {Object.keys(gruppertHistorikk).map((tiltakId) => {
        const tiltakHistorikk = gruppertHistorikk[tiltakId];
        const sistDelt = tiltakHistorikk[0];
        if (!sistDelt) return null;

        return (
          <Accordion key={tiltakId}>
            <Accordion.Item>
              <Accordion.Header>
                {sistDelt.navn} - Sist delt {formaterDato(sistDelt.createdAt)}
              </Accordion.Header>
              <Accordion.Content>
                <Table size="small">
                  <Table.Header>
                    <Table.Row>
                      <Table.HeaderCell scope="col"></Table.HeaderCell>
                      <Table.HeaderCell scope="col"></Table.HeaderCell>
                    </Table.Row>
                  </Table.Header>
                  <Table.Body>
                    {tiltakHistorikk.map(({ createdAt, dialogId }, index) => {
                      return (
                        <Table.Row key={dialogId}>
                          <Table.DataCell>
                            {formaterDato(createdAt)} {index === 0 ? " - Siste melding delt" : null}
                          </Table.DataCell>
                          <Table.DataCell>
                            <Button
                              variant="tertiary-neutral"
                              onClick={(e) => {
                                e.preventDefault();
                                navigateToModiaApp({
                                  route: ModiaRoute.DIALOG,
                                  dialogId,
                                });
                              }}
                            >
                              Gå til dialogen
                            </Button>
                          </Table.DataCell>
                        </Table.Row>
                      );
                    })}
                  </Table.Body>
                </Table>
              </Accordion.Content>
            </Accordion.Item>
          </Accordion>
        );
      })}
    </VStack>
  );
}
