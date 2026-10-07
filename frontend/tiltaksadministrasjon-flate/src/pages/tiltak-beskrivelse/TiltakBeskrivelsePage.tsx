import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Box, Tabs } from "@navikt/ds-react";
import React, { Suspense } from "react";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useTiltakBeskrivelse } from "@/api/tiltak-beskrivelse/useTiltakBeskrivelse";
import { Outlet, useLocation } from "react-router";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { TiltakBeskrivelseIkon } from "@/components/ikoner/TiltakBeskrivelseIkon";
import { Laster } from "@/components/laster/Laster";
import { TiltakBeskrivelseHandlinger } from "@/components/tiltak-beskrivelse/TiltakBeskrivelseHandlinger";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";

const TABS = [
  { key: "detaljer", label: "Detaljer" },
  { key: "redaksjonelt-innhold", label: "Informasjon for veiledere" },
] as const;

type TabKey = (typeof TABS)[number]["key"];

function getCurrentTab(pathname: string): TabKey {
  return pathname.includes("redaksjonelt-innhold") ? "redaksjonelt-innhold" : "detaljer";
}

function createTabUrl(tiltakBeskrivelseId: string, tabKey: TabKey): string {
  return tabKey === "detaljer"
    ? `/tiltak-beskrivelser/${tiltakBeskrivelseId}`
    : `/tiltak-beskrivelser/${tiltakBeskrivelseId}/${tabKey}`;
}

export function TiltakBeskrivelsePage() {
  const { tiltakBeskrivelseId } = useRequiredParams(["tiltakBeskrivelseId"]);
  const { data: tiltakBeskrivelse } = useTiltakBeskrivelse(tiltakBeskrivelseId);
  const { data: ansatt } = useHentAnsatt();
  const { pathname } = useLocation();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();

  const currentTab = getCurrentTab(pathname);

  const brodsmuler: Brodsmule[] = [
    { tittel: "Tiltaksbeskrivelser", lenke: "/tiltak-beskrivelser" },
    {
      tittel: "Tiltaksbeskrivelse",
      lenke: currentTab === "detaljer" ? undefined : `/tiltak-beskrivelser/${tiltakBeskrivelseId}`,
    },
    currentTab === "redaksjonelt-innhold" ? { tittel: "Informasjon for veiledere" } : undefined,
  ].filter(Boolean) as Brodsmule[];

  return (
    <>
      <title>{`Tiltaksbeskrivelse | ${tiltakBeskrivelse.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner ikon={<TiltakBeskrivelseIkon />} heading={tiltakBeskrivelse.navn} />
      <Tabs value={currentTab}>
        <Box background="default">
          <Tabs.List>
            {TABS.map((tab) => (
              <Tabs.Tab
                key={tab.key}
                value={tab.key}
                label={tab.label}
                onClick={() => navigateAndReplaceUrl(createTabUrl(tiltakBeskrivelseId, tab.key))}
              />
            ))}
          </Tabs.List>
        </Box>
        <React.Suspense fallback={<Laster tekst="Laster innhold..." />}>
          <WhitePaddedBox>
            <InlineErrorBoundary>
              <Suspense fallback={<Laster tekst="Laster handlinger..." />}>
                <TiltakBeskrivelseHandlinger
                  ansatt={ansatt}
                  tiltakBeskrivelse={tiltakBeskrivelse}
                />
              </Suspense>
            </InlineErrorBoundary>
            <Separator />
            <Tabs.Panel value={currentTab} data-testid="tiltak-beskrivelse_info-container">
              <Outlet />
            </Tabs.Panel>
          </WhitePaddedBox>
        </React.Suspense>
      </Tabs>
    </>
  );
}
