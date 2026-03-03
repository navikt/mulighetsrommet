import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { BankNoteFillIcon } from "@navikt/aksel-icons";
import { Heading, HStack, VStack } from "@navikt/ds-react";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Outlet } from "react-router";

export function UtbetalingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing } = useGjennomforing(gjennomforingId);

  const brodsmuler: Brodsmule[] = [
    { tittel: "Gjennomføringer", lenke: `/gjennomforinger` },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforing.id}`,
    },
    {
      tittel: "Utbetalinger",
      lenke: `/gjennomforinger/${gjennomforing.id}/utbetalinger`,
    },
    { tittel: "Utbetaling" },
  ];

  return (
    <>
      <title>{utbetalingTekster.title}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HStack gap="space-8" className="bg-ax-bg-default border-b-2 border-ax-neutral-300 p-2">
        <BankNoteFillIcon color="#2AA758" className="w-10 h-10" />
        <Heading size="large" level="1">
          {utbetalingTekster.header(gjennomforing.navn)}
        </Heading>
      </HStack>
      <ContentBox>
        <VStack gap="space-16" padding="space-8" className="bg-ax-bg-default">
          <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
          <Outlet />
        </VStack>
      </ContentBox>
    </>
  );
}
