import { Header } from "@/components/detaljside/Header";
import { TiltakstypeIkon } from "@/components/ikoner/TiltakstypeIkon";
import { Laster } from "@/components/laster/Laster";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Box, Heading, Tabs } from "@navikt/ds-react";
import React from "react";
import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Outlet, useLocation } from "react-router";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";

export function TiltakstypePage() {
  const { tiltakstypeId } = useRequiredParams(["tiltakstypeId"]);
  const { data: tiltakstype } = useTiltakstypeById();
  const [currentTab, tabs] = useTabs(tiltakstypeId);

  const brodsmuler: (Brodsmule | undefined)[] = [
    { tittel: "Tiltakstyper", lenke: "/tiltakstyper" },
    {
      tittel: "Tiltakstype",
      lenke: currentTab === "detaljer" ? undefined : `/tiltakstyper/${tiltakstypeId}`,
    },
    currentTab === "redaksjonelt-innhold" ? { tittel: "Redaksjonelt innhold" } : undefined,
  ];

  return (
    <>
      <title>{`Tiltakstype | ${tiltakstype.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <TiltakstypeIkon />
        <Heading size="large" level="2">
          {tiltakstype.navn}
        </Heading>
      </Header>
      <Tabs value={currentTab}>
        <Box background="default">
          <Tabs.List>
            {tabs.map((tab) => (
              <Tabs.Tab key={tab.key} value={tab.key} label={tab.label} onClick={tab.onClick} />
            ))}
          </Tabs.List>
        </Box>
        <React.Suspense fallback={<Laster tekst="Laster innhold..." />}>
          <WhitePaddedBox>
            <Tabs.Panel value={currentTab}>
              <Outlet />
            </Tabs.Panel>
          </WhitePaddedBox>
        </React.Suspense>
      </Tabs>
    </>
  );
}

interface Tab {
  key: string;
  label: string;
  onClick: () => void;
}

const TAB_KEYS = ["redaksjonelt-innhold"] as const;

function getCurrentTab(pathname: string): string {
  const tabKey = TAB_KEYS.find((key) => pathname.includes(key));
  return tabKey ?? "detaljer";
}

function createTabUrl(tiltakstypeId: string, tabKey: string): string {
  return tabKey === "detaljer"
    ? `/tiltakstyper/${tiltakstypeId}`
    : `/tiltakstyper/${tiltakstypeId}/${tabKey}`;
}

function useTabs(tiltakstypeId: string): [string, Tab[]] {
  const { pathname } = useLocation();
  const currentTab = getCurrentTab(pathname);
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();

  const tabConfigs = [
    { key: "detaljer", label: "Detaljer" },
    { key: "redaksjonelt-innhold", label: "Redaksjonelt innhold" },
  ];

  const tabs: Tab[] = tabConfigs.map(({ key, label }) => ({
    key,
    label,
    onClick: () => navigateAndReplaceUrl(createTabUrl(tiltakstypeId, key)),
  }));

  return [currentTab, tabs];
}
