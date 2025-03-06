import { Header } from "@/components/detaljside/Header";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { TilsagnRequest } from "@mr/api-client-v2";
import { Alert, Heading, VStack } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router";
import { useAvtale } from "../../../../api/avtaler/useAvtale";
import { useAdminGjennomforingById } from "../../../../api/gjennomforing/useAdminGjennomforingById";
import { Laster } from "../../../../components/laster/Laster";
import { tilsagnQuery } from "../detaljer/tilsagnDetaljerLoader";
import { godkjenteTilsagnQuery } from "../opprett/opprettTilsagnLoader";
import { TilsagnTabell } from "../tabell/TilsagnTabell";
export function RedigerTilsagnFormPage() {
  const { gjennomforingId, tilsagnId } = useParams();

  const { data: avtale } = useAvtale(gjennomforingId);
  const { data: gjennomforing } = useAdminGjennomforingById();
  const { data: tilsagnData } = useQuery({ ...tilsagnQuery(tilsagnId) });
  const { data: godkjenteTilsagn } = useQuery({
    ...godkjenteTilsagnQuery(gjennomforingId),
  });

  const tilsagn = tilsagnData?.data;

  if (!gjennomforing || !tilsagn || !godkjenteTilsagn || !avtale) {
    return <Laster />;
  }

  const brodsmuler: Array<Brodsmule | undefined> = [
    {
      tittel: "Gjennomføringer",
      lenke: `/gjennomforinger`,
    },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforing.id}`,
    },
    {
      tittel: "Rediger tilsagn",
    },
  ];

  const defaults: TilsagnRequest = {
    id: tilsagn.id,
    type: tilsagn.type,
    periodeStart: tilsagn.periodeStart,
    periodeSlutt: tilsagn.periodeSlutt,
    kostnadssted: tilsagn.kostnadssted.enhetsnummer,
    beregning: tilsagn.beregning.input,
    gjennomforingId: gjennomforing.id,
  };

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <GjennomforingIkon />
        <Heading size="large" level="2">
          Rediger tilsagn
        </Heading>
      </Header>
      <ContentBox>
        <VStack gap={"8"}>
          <WhitePaddedBox>
            <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
          </WhitePaddedBox>
          <WhitePaddedBox>
            <TilsagnFormContainer
              avtale={avtale}
              gjennomforing={gjennomforing}
              defaults={defaults}
            />
          </WhitePaddedBox>
          <WhitePaddedBox>
            <VStack gap="4">
              <Heading size="medium">Aktive tilsagn</Heading>
              {godkjenteTilsagn.data.length > 0 ? (
                <TilsagnTabell tilsagn={godkjenteTilsagn.data} />
              ) : (
                <Alert variant="info">Det finnes ingen tilsagn for dette tiltaket</Alert>
              )}
            </VStack>
          </WhitePaddedBox>
        </VStack>
      </ContentBox>
    </main>
  );
}
