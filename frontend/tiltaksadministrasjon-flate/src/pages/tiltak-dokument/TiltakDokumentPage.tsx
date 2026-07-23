import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Box, Tabs } from "@navikt/ds-react";
import React, { Suspense } from "react";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useTiltakDokument } from "@/api/tiltak-dokument/useTiltakDokument";
import { Outlet, useLocation } from "react-router";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { TiltakDokumentIkon } from "@/components/ikoner/TiltakDokumentIkon";
import { Laster } from "@/components/laster/Laster";
import { TiltakDokumentHandlinger } from "@/components/tiltak-dokument/TiltakDokumentHandlinger";
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

function createTabUrl(tiltakDokumentId: string, tabKey: TabKey): string {
  return tabKey === "detaljer"
    ? `/tiltak-dokumenter/${tiltakDokumentId}`
    : `/tiltak-dokumenter/${tiltakDokumentId}/${tabKey}`;
}

export function TiltakDokumentPage() {
  const { tiltakDokumentId } = useRequiredParams(["tiltakDokumentId"]);
  const { data: tiltakDokument } = useTiltakDokument(tiltakDokumentId);
  const { data: ansatt } = useHentAnsatt();
  const { pathname } = useLocation();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();

  const currentTab = getCurrentTab(pathname);

  const brodsmuler: Brodsmule[] = [
    { tittel: "Tiltaksdokumenter", lenke: "/tiltak-dokumenter" },
    {
      tittel: "Tiltaksdokument",
      lenke: currentTab === "detaljer" ? undefined : `/tiltak-dokumenter/${tiltakDokumentId}`,
    },
    currentTab === "redaksjonelt-innhold" ? { tittel: "Informasjon for veiledere" } : undefined,
  ].filter(Boolean) as Brodsmule[];

  return (
    <>
      <title>{`Tiltaksdokument | ${tiltakDokument.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner ikon={<TiltakDokumentIkon />} heading={tiltakDokument.navn} />
      <Tabs value={currentTab}>
        <Box background="default">
          <Tabs.List>
            {TABS.map((tab) => (
              <Tabs.Tab
                key={tab.key}
                value={tab.key}
                label={tab.label}
                onClick={() => navigateAndReplaceUrl(createTabUrl(tiltakDokumentId, tab.key))}
              />
            ))}
          </Tabs.List>
        </Box>
        <React.Suspense fallback={<Laster tekst="Laster innhold..." />}>
          <WhitePaddedBox>
            <InlineErrorBoundary>
              <Suspense fallback={<Laster tekst="Laster handlinger..." />}>
                <TiltakDokumentHandlinger ansatt={ansatt} tiltakDokument={tiltakDokument} />
              </Suspense>
            </InlineErrorBoundary>
            <Separator />
            <Tabs.Panel value={currentTab} data-testid="tiltak-dokument_info-container">
              <Outlet />
            </Tabs.Panel>
          </WhitePaddedBox>
        </React.Suspense>
      </Tabs>
    </>
  );
}
