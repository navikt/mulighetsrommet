import { Header } from "@/components/detaljside/Header";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Laster } from "@/components/laster/Laster";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Heading, Tabs } from "@navikt/ds-react";
import React from "react";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Outlet, useLocation } from "react-router";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { FeatureToggle, GjennomforingDto } from "@tiltaksadministrasjon/api-client";
import { DataElementStatusTag } from "@mr/frontend-common";
import { isGruppetiltak } from "@/api/gjennomforing/utils";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";

export function GjennomforingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing } = useGjennomforing(gjennomforingId);

  const { data: enableTilskuddsbehandling } = useFeatureToggle(
    FeatureToggle.TILTAKSADMINISTRASJON_VIS_TILSKUDDSBEHANDLING,
  );
  const [currentTab, tabs] = useTabs(gjennomforing, !!enableTilskuddsbehandling);

  const brodsmuler: (Brodsmule | undefined)[] = [
    {
      tittel: "Gjennomføringer",
      lenke: `/gjennomforinger`,
    },
    {
      tittel: "Gjennomføring",
      lenke: currentTab === "detaljer" ? undefined : `/gjennomforinger/${gjennomforing.id}`,
    },
    currentTab === "tilskuddsbehandlinger" ? { tittel: "Tilskuddsbehandlinger" } : undefined,
    currentTab === "tilsagn" ? { tittel: "Tilsagnoversikt" } : undefined,
    currentTab === "redaksjonelt-innhold" ? { tittel: "Informasjon til veilederene" } : undefined,
    currentTab === "utbetalinger" ? { tittel: "Utbetalinger" } : undefined,
    currentTab === "deltakerliste" ? { tittel: "Deltakerliste" } : undefined,
  ];

  return (
    <>
      <title>{`Gjennomføring | ${gjennomforing.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <GjennomforingIkon />
        <Heading size="large" level="2">
          {gjennomforing.navn}
        </Heading>
        <DataElementStatusTag {...gjennomforing.status.status} />
      </Header>
      <Tabs value={currentTab}>
        <Tabs.List className="bg-ax-bg-default">
          {tabs.map((tab) => (
            <Tabs.Tab key={tab.key} value={tab.key} label={tab.label} onClick={tab.onClick} />
          ))}
        </Tabs.List>
        <React.Suspense fallback={<Laster tekst="Laster innhold..." />}>
          <ContentBox>
            <WhitePaddedBox>
              <Tabs.Panel value={currentTab} data-testid="gjennomforing_info-container">
                <Outlet />
              </Tabs.Panel>
            </WhitePaddedBox>
          </ContentBox>
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

interface TabConfig {
  key: string;
  label: string;
}

const GRUPPETILTAK_TABS: TabConfig[] = [
  { key: "detaljer", label: "Detaljer" },
  { key: "redaksjonelt-innhold", label: "Informasjon for veiledere" },
  { key: "tilsagn", label: "Tilsagn" },
  { key: "utbetalinger", label: "Utbetalinger" },
  { key: "deltakerliste", label: "Deltakerliste" },
];

const STANDARD_TABS: TabConfig[] = [
  { key: "detaljer", label: "Detaljer" },
  {
    key: "tilskuddsbehandlinger",
    label: "Tilskuddsbehandlinger",
  },
  { key: "tilsagn", label: "Tilsagn" },
  { key: "utbetalinger", label: "Utbetalinger" },
];

const TAB_KEYS = [
  "tilskuddsbehandlinger",
  "tilsagn",
  "redaksjonelt-innhold",
  "deltakerliste",
  "utbetalinger",
] as const;

function createTabUrl(gjennomforingId: string, tabKey: string): string {
  return tabKey === "detaljer"
    ? `/gjennomforinger/${gjennomforingId}`
    : `/gjennomforinger/${gjennomforingId}/${tabKey}`;
}

function getCurrentTab(pathname: string): string {
  const tabKey = TAB_KEYS.find((key) => pathname.includes(key));
  return tabKey || "detaljer";
}

function useTabs(
  gjennomforing: GjennomforingDto,
  enableTilskuddsbehandling: boolean,
): [string, Tab[]] {
  const { pathname } = useLocation();
  const currentTab = getCurrentTab(pathname);
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();

  const tabConfigs = isGruppetiltak(gjennomforing) ? GRUPPETILTAK_TABS : STANDARD_TABS;
  const filteredTabConfigs = enableTilskuddsbehandling
    ? tabConfigs
    : tabConfigs.filter((tab) => tab.key !== "tilskuddsbehandlinger");

  const tabs: Tab[] = filteredTabConfigs.map(({ key, label }) => ({
    key,
    label,
    onClick: () => navigateAndReplaceUrl(createTabUrl(gjennomforing.id, key)),
  }));

  return [currentTab, tabs];
}
