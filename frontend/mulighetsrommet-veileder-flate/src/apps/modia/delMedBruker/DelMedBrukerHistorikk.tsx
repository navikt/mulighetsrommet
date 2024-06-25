import { Accordion, Alert, Button, HGrid, Skeleton, VStack } from "@navikt/ds-react";
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
    <VStack gap="2">
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
                <ul>
                  {tiltakHistorikk.map((delt, index) => {
                    return (
                      <li key={delt.dialogId}>
                        <HGrid columns={3} align="center">
                          <div>
                            {formaterDato(delt.createdAt)}
                            {index === 0 ? " - Siste melding delt" : null}
                          </div>
                          <div>
                            <Button
                              variant="tertiary-neutral"
                              onClick={(e) => {
                                e.preventDefault();
                                navigateToModiaApp({
                                  route: ModiaRoute.DIALOG,
                                  dialogId: delt.dialogId,
                                });
                              }}
                            >
                              Gå til dialogen
                            </Button>
                          </div>
                        </HGrid>
                      </li>
                    );
                  })}
                </ul>
              </Accordion.Content>
            </Accordion.Item>
          </Accordion>
        );
      })}
    </VStack>
  );
}
