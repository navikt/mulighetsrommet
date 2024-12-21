import { Header } from "@/components/detaljside/Header";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { Box, Heading, HStack } from "@navikt/ds-react";
import { useLoaderData, useMatch, useParams } from "react-router";
import { refusjonskravDetaljerLoader } from "./refusjonskravDetaljerLoader";

export function RefusjonskravDetaljer() {
  const { gjennomforing, refusjonskrav } = useLoaderData<typeof refusjonskravDetaljerLoader>();

  const { avtaleId, tiltaksgjennomforingId } = useParams();
  const erPaaGjennomforingerForAvtale = useMatch(
    "/avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/opprett-tilsagn",
  );

  const brodsmuler: Array<Brodsmule | undefined> = [
    { tittel: "Forside", lenke: "/" },
    avtaleId
      ? { tittel: "Avtaler", lenke: "/avtaler" }
      : { tittel: "Tiltaksgjennomføringer", lenke: "/tiltaksgjennomforinger" },
    avtaleId
      ? {
          tittel: "Avtaledetaljer",
          lenke: `/avtaler/${avtaleId}`,
        }
      : undefined,
    erPaaGjennomforingerForAvtale
      ? {
          tittel: "Avtalens gjennomføringer",
          lenke: `/avtaler/${avtaleId}/tiltaksgjennomforinger`,
        }
      : undefined,
    {
      tittel: "Tiltaksgjennomføringdetaljer",
      lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}`,
    },
    {
      tittel: "Refusjonskravoversikt",
      lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}/refusjonskrav`,
    },
    {
      tittel: "Refusjonskravdetaljer",
      lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}/refusjonskrav`,
    },
  ];

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <TiltaksgjennomforingIkon />
        <Heading size="large" level="2">
          <HStack gap="2" align={"center"}>
            Refusjonskrav for {gjennomforing.navn}
          </HStack>
        </Heading>
      </Header>
      <ContainerLayout>
        <Box background="bg-default" padding={"5"}>
          <Box
            borderWidth="2"
            borderColor="border-subtle"
            marginBlock={"4 10"}
            borderRadius={"medium"}
            padding={"2"}
          >
            {refusjonskrav.status}
          </Box>
        </Box>
      </ContainerLayout>
    </main>
  );
}
