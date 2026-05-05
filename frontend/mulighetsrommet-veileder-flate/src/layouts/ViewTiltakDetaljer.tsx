import { Oppskrift } from "@/components/oppskrift/Oppskrift";
import { PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { Box, HGrid, HStack, Page, Tabs, VStack } from "@navikt/ds-react";
import { VeilederflateTiltak } from "@api-client";
import { ReactNode, Suspense, useState } from "react";
import SidemenyInfo from "@/components/sidemeny/SidemenyInfo";
import { TiltakDetaljer } from "@/components/tabs/TiltakDetaljer";
import { TiltakHeader } from "./TiltakHeader";
import { useInnsatsgrupper } from "@/api/queries/useInnsatsgrupper";
import { EstimertVentetid } from "@/components/sidemeny/EstimertVentetid";
import { SidemenyKanKombineresMed } from "@/components/sidemeny/SidemenyKanKombineresMed";
import { DetaljerSkeleton } from "@mr/frontend-common";
import { isTiltakGruppe } from "@/api/queries/useArbeidsmarkedstiltakById";
import { Melding } from "@/components/melding/Melding";

interface Props {
  tiltak: VeilederflateTiltak;
  brukerActions: ReactNode;
  knapperad: ReactNode;
}

export function ViewTiltakDetaljer({ tiltak, brukerActions, knapperad }: Props) {
  const { data: innsatsgrupper } = useInnsatsgrupper();

  const [oppskriftId, setOppskriftId] = useState<string | undefined>(undefined);

  const harKombinasjon = tiltak.tiltakstype.kanKombineresMed.length > 0;

  return (
    <Page.Block gutters>
      <HStack justify="space-between">{knapperad}</HStack>
      <Suspense fallback={<DetaljerSkeleton />}>
        <Box padding="space-24" background="default">
          <HGrid gap="space-128" columns="1fr 0.5fr" id="gjennomforing_detaljer">
            <VStack gap="space-16">
              <TiltakHeader tiltak={tiltak} />
              <TiltakDetaljer tiltak={tiltak} setOppskriftId={setOppskriftId} />
            </VStack>
            <VStack gap="space-16">
              {isTiltakGruppe(tiltak) && tiltak.apentForPamelding && (
                <PadlockLockedFillIcon
                  title="Tiltaket er stengt for påmelding"
                  width="2rem"
                  height="2rem"
                />
              )}
              {isTiltakGruppe(tiltak) && tiltak.estimertVentetid && (
                <EstimertVentetid estimertVentetid={tiltak.estimertVentetid} />
              )}
              <Tabs size="small" defaultValue="info">
                <Tabs.List>
                  <Tabs.Tab value="info" label="Info" />
                  {harKombinasjon ? (
                    <Tabs.Tab value="kombineres" label="Kan kombineres med" />
                  ) : null}
                </Tabs.List>
                <Tabs.Panel value="info">
                  <SidemenyInfo tiltak={tiltak} innsatsgrupper={innsatsgrupper} />
                </Tabs.Panel>
                {harKombinasjon ? (
                  <Tabs.Panel value="kombineres">
                    <SidemenyKanKombineresMed tiltak={tiltak} />
                  </Tabs.Panel>
                ) : null}
              </Tabs>
              {tiltak.oppmoteSted && (
                <Melding header="Oppmøtested" variant="info">
                  {tiltak.oppmoteSted}
                </Melding>
              )}
              <VStack gap="space-16">{brukerActions}</VStack>
            </VStack>
          </HGrid>
          {oppskriftId && (
            <Oppskrift
              oppskriftId={oppskriftId}
              tiltakskode={tiltak.tiltakstype.tiltakskode}
              setOppskriftId={setOppskriftId}
            />
          )}
        </Box>
      </Suspense>
    </Page.Block>
  );
}
