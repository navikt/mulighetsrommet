import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { PiggybankFillIcon } from "@navikt/aksel-icons";
import { Outlet, useParams } from "react-router";
import { AktiveTilsagnTable } from "@/pages/gjennomforing/tilsagn/tabell/TilsagnTable";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { VStack } from "@navikt/ds-react";
import { GjennomforingHeader } from "@/components/gjennomforing/GjennomforingHeader";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";

export function TilsagnPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { tilsagnId } = useParams();
  const { gjennomforing } = useGjennomforing(gjennomforingId);

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
      <HeaderBanner
        ikon={<PiggybankFillIcon color="#FFAA33" width="2.5rem" height="2.5rem" />}
        heading={tilsagnId ? `Tilsagn for ${gjennomforing.navn}` : "Opprett tilsagn"}
      />
      <GjennomforingHeader gjennomforingId={gjennomforingId} />
      <WhitePaddedBox>
        <Outlet />
      </WhitePaddedBox>
      <VStack padding="space-8" className="bg-ax-bg-default overflow-x-scroll">
        <AktiveTilsagnTable gjennomforingId={gjennomforingId} />
      </VStack>
    </>
  );
}
