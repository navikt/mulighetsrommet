import { VStack } from "@navikt/ds-react";
import { Header } from "@/components/detaljside/Header";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { Heading, HStack } from "@navikt/ds-react";
import { useLoaderData } from "react-router";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { utbetalingDetaljerPageLoader } from "./utbetalingDetaljerPageLoader";

export function UtbetalingDetaljerPage() {
  const { gjennomforing, utbetaling } = useLoaderData<typeof utbetalingDetaljerPageLoader>();

  const brodsmuler: Brodsmule[] = [
    { tittel: "Gjennomføringer", lenke: `/gjennomforinger` },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforing.id}`,
    },
    {
      tittel: "Refusjonskravoversikt",
      lenke: `/gjennomforinger/${gjennomforing.id}/refusjonskrav`,
    },
    { tittel: "Utbetaling" },
  ];

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <GjennomforingIkon />
        <Heading size="large" level="2">
          <HStack gap="2" align={"center"}>
            Utbetalingskrav for {gjennomforing.navn}
          </HStack>
        </Heading>
      </Header>
      <ContentBox>
        <WhitePaddedBox>
          <VStack>
            <div>Utbetaling detaljer side</div>
            <div>{utbetaling.krav.id}</div>
            <div>Not implemented</div>
          </VStack>
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}
