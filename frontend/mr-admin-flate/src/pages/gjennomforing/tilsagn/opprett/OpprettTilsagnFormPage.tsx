import { Header } from "@/components/detaljside/Header";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Prismodell, TilsagnType } from "@mr/api-client-v2";
import { Alert, Heading, VStack } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useParams, useSearchParams } from "react-router";
import { useAvtale } from "../../../../api/avtaler/useAvtale";
import { Laster } from "../../../../components/laster/Laster";
import { TilsagnTabell } from "../tabell/TilsagnTabell";
import { godkjenteTilsagnQuery, tilsagnDefaultsQuery } from "./opprettTilsagnLoader";
import { useAdminGjennomforingById } from "../../../../api/gjennomforing/useAdminGjennomforingById";

export function OpprettTilsagnFormPage() {
  const [searchParams] = useSearchParams();
  const type = (searchParams.get("type") as TilsagnType) ?? TilsagnType.TILSAGN;
  const periodeStart = searchParams.get("periodeStart");
  const periodeSlutt = searchParams.get("periodeSlutt");
  const belop = searchParams.get("belop");
  const prismodell = searchParams.get("prismodell")
    ? (searchParams.get("prismodell") as Prismodell)
    : null;
  const kostnadssted = searchParams.get("kostnadssted");

  const { gjennomforingId } = useParams();
  const { data: gjennomforing } = useAdminGjennomforingById();
  const { data: avtale } = useAvtale(gjennomforing?.avtaleId);
  const { data: defaults } = useQuery({
    ...tilsagnDefaultsQuery({
      gjennomforingId,
      type,
      prismodell,
      periodeStart,
      periodeSlutt,
      belop: belop ? Number(belop) : null,
      kostnadssted,
    }),
  });
  const { data: godkjenteTilsagn } = useQuery({
    ...godkjenteTilsagnQuery(gjennomforingId),
  });

  if (!gjennomforing || !avtale || !defaults || !godkjenteTilsagn) {
    return <Laster tekst="Laster gjennomføring..." />;
  }

  const brodsmuler: Array<Brodsmule | undefined> = [
    {
      tittel: "Gjennomføringer",
      lenke: "/gjennomforinger",
    },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforing.id}`,
    },
    {
      tittel: "Opprett tilsagn",
    },
  ];

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <GjennomforingIkon />
        <Heading size="large" level="2">
          Opprett tilsagn
        </Heading>
      </Header>
      <ContentBox>
        <VStack gap={"8"}>
          <WhitePaddedBox>
            <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
            <TilsagnFormContainer
              avtale={avtale}
              gjennomforing={gjennomforing}
              defaults={defaults.data}
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
