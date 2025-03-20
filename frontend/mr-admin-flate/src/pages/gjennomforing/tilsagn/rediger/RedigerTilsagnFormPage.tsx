import { Header } from "@/components/detaljside/Header";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { TilsagnRequest } from "@mr/api-client-v2";
import { Alert, Heading, VStack } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { useParams } from "react-router";
import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { tilsagnQuery } from "../detaljer/tilsagnDetaljerLoader";
import { godkjenteTilsagnQuery } from "../opprett/opprettTilsagnLoader";
import { TilsagnTabell } from "../tabell/TilsagnTabell";
import { Laster } from "@/components/laster/Laster";
import { formaterDatoSomYYYYMMDD, subtractDays } from "@/utils/Utils";

function useRedigerTilsagnFormData() {
  const { gjennomforingId, tilsagnId } = useParams();
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: avtale } = usePotentialAvtale(gjennomforing?.avtaleId);
  const { data: tilsagnDetaljer } = useSuspenseQuery({ ...tilsagnQuery(tilsagnId) });
  const { data: godkjenteTilsagn } = useSuspenseQuery({
    ...godkjenteTilsagnQuery(gjennomforingId),
  });

  return {
    avtale,
    gjennomforing,
    godkjenteTilsagn: godkjenteTilsagn.data.filter((t) => t.id !== tilsagnDetaljer.data.tilsagn.id),
    tilsagn: tilsagnDetaljer.data.tilsagn,
  };
}

export function RedigerTilsagnFormPage() {
  const { gjennomforingId } = useParams();
  const { avtale, gjennomforing, godkjenteTilsagn, tilsagn } = useRedigerTilsagnFormData();

  const brodsmuler: Array<Brodsmule | undefined> = [
    {
      tittel: "Gjennomføringer",
      lenke: `/gjennomforinger`,
    },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforingId}`,
    },
    {
      tittel: "Rediger tilsagn",
    },
  ];

  const defaults: TilsagnRequest = {
    id: tilsagn.id,
    type: tilsagn.type,
    periodeStart: tilsagn.periode.start,
    periodeSlutt: formaterDatoSomYYYYMMDD(subtractDays(tilsagn.periode.slutt, 1)),
    kostnadssted: tilsagn.kostnadssted.enhetsnummer,
    beregning: tilsagn.beregning.input,
    gjennomforingId: gjennomforing.id,
  };

  if (!avtale) {
    return <Laster tekst="Laster data..." />;
  }

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
              {godkjenteTilsagn.length > 0 ? (
                <TilsagnTabell tilsagn={godkjenteTilsagn} />
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
