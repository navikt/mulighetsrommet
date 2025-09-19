import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { PiggybankFillIcon } from "@navikt/aksel-icons";
import { Outlet, useParams } from "react-router";
import { AktiveTilsagnTable } from "@/pages/gjennomforing/tilsagn/tabell/TilsagnTable";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Heading, HStack, VStack } from "@navikt/ds-react";

export function TilsagnPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { tilsagnId } = useParams();
  const { data: gjennomforing } = useGjennomforing(gjennomforingId);

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
      tittel: "Tilsagnsoversikt",
      lenke: `/gjennomforinger/${gjennomforing.id}/tilsagn`,
    },
    {
      tittel: "Tilsagn",
    },
  ];

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HStack gap="2" className="bg-white border-b-2 border-gray-200 p-2">
        <PiggybankFillIcon color="#FFAA33" className="w-10 h-10" />
        <Heading size="large" level="2">
          {tilsagnId ? `Tilsagn for ${gjennomforing.navn}` : "Opprett tilsagn"}
        </Heading>
      </HStack>
      <ContentBox>
        <VStack gap="6" padding="4" className="bg-white">
          <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
          <Outlet />
        </VStack>
      </ContentBox>
      <VStack padding="4" className="bg-white overflow-x-scroll">
        <AktiveTilsagnTable gjennomforingId={gjennomforingId} />
      </VStack>
    </>
  );
}
