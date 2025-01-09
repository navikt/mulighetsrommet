import { Header } from "@/components/detaljside/Header";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { Heading, HStack } from "@navikt/ds-react";
import { useLoaderData, useMatch, useParams } from "react-router";
import { refusjonskravDetaljerLoader } from "./refusjonskravDetaljerLoader";
import { DetaljerInfoContainer } from "@/pages/DetaljerInfoContainer";
import { Bolk } from "@/components/detaljside/Bolk";
import { Metadata } from "@/components/detaljside/Metadata";
import { formaterDato } from "@/utils/Utils";
import { RefusjonskravStatusTag } from "../RefusjonskravStatusTag";

export function RefusjonskravDetaljer() {
  const { gjennomforing, refusjonskrav } = useLoaderData<typeof refusjonskravDetaljerLoader>();

  const { avtaleId, tiltaksgjennomforingId } = useParams();
  const erPaaGjennomforingerForAvtale = useMatch(
    "/avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/opprett-tilsagn",
  );

  const brodsmuler: Array<Brodsmule | undefined> = [
    { tittel: "", lenke: "/" },
    avtaleId
      ? { tittel: "Avtaler", lenke: "/avtaler" }
      : { tittel: "Gjennomføringer", lenke: "/tiltaksgjennomforinger" },
    avtaleId
      ? {
          tittel: "Avtale",
          lenke: `/avtaler/${avtaleId}`,
        }
      : undefined,
    erPaaGjennomforingerForAvtale
      ? {
          tittel: "Gjennomføringer",
          lenke: `/avtaler/${avtaleId}/tiltaksgjennomforinger`,
        }
      : undefined,
    {
      tittel: "Gjennomføring",
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
        <HStack padding="5">
          <DetaljerInfoContainer withBorderRight>
            <Bolk>
              <Metadata
                header="Refusjonskravperiode"
                verdi={`${formaterDato(refusjonskrav.beregning.periodeStart)} - ${formaterDato(refusjonskrav.beregning.periodeSlutt)}`}
              />
              <Metadata
                header="Status"
                verdi={<RefusjonskravStatusTag status={refusjonskrav.status} />}
              />
            </Bolk>
            <Bolk>
              <Metadata header="Beløp" verdi={refusjonskrav.beregning.belop} />
            </Bolk>
          </DetaljerInfoContainer>
        </HStack>
      </ContainerLayout>
    </main>
  );
}
