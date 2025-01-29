import { Header } from "@/components/detaljside/Header";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { Alert, Heading, HStack } from "@navikt/ds-react";
import { useLoaderData } from "react-router";
import { utbetalingskravPageLoader } from "./utbetalingskravPageLoader";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { UtbetalingDetaljer } from "./UtbetalingDetaljer";
import { BehandleUtbetalingForm } from "./BehandleUtbetalingForm";

export function UtbetalingskravPage() {
  const { gjennomforing, utbetaling } = useLoaderData<typeof utbetalingskravPageLoader>();

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
    { tittel: "Behandle utbetaling" },
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
          {utbetaling.type === "UTBETALING" && <UtbetalingDetaljer utbetaling={utbetaling} />}
          {utbetaling.type === "BEHANDLE_UTBETALING" &&
            (utbetaling.tilsagn.length === 0 ? (
              <Alert variant="info">Tilsagn mangler</Alert>
            ) : (
              <BehandleUtbetalingForm gjennomforingId={gjennomforing.id} behandling={utbetaling} />
            ))}
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}
