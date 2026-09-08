import { Laster } from "@/components/laster/Laster";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Box, Tabs } from "@navikt/ds-react";
import React from "react";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Outlet, useLocation } from "react-router";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import {
  FeatureToggle,
  GjennomforingDetaljerDto,
  PrismodellType,
  TotrinnskontrollDto,
} from "@tiltaksadministrasjon/api-client";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { GjennomforingEnkeltplassIkon } from "@/components/ikoner/GjennomforingEnkeltplassIkon";
import { GjennomforingAvtaleIkon } from "@/components/ikoner/GjennomforingAvtaleIkon";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { erGodkjent } from "@/utils/totrinnskontroll";
import { isGjennomforingAvtaleDetaljer } from "@/api/gjennomforing/utils";
import { GjennomforingEnkeltplassHeader } from "@/components/gjennomforing/GjennomforingEnkeltplassHeader";

export function GjennomforingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const detaljer = useGjennomforing(gjennomforingId);

  const { data: enableTilskuddsbehandling } = useFeatureToggle(
    FeatureToggle.TILTAKSADMINISTRASJON_VIS_TILSKUDDSBEHANDLING,
  );
  const [currentTab, tabs] = useTabs(detaljer, !!enableTilskuddsbehandling);

  const brodsmuler: (Brodsmule | undefined)[] = [
    {
      tittel: "Gjennomføringer",
      lenke: `/gjennomforinger`,
    },
    {
      tittel: "Gjennomføring",
      lenke:
        currentTab === "detaljer" ? undefined : `/gjennomforinger/${detaljer.gjennomforing.id}`,
    },
    currentTab === "tilskudd-behandling" ? { tittel: "Tilskuddsbehandlinger" } : undefined,
    currentTab === "tilskudd-utbetalinger" ? { tittel: "Utbetalinger" } : undefined,
    currentTab === "tilsagn" ? { tittel: "Tilsagnoversikt" } : undefined,
    currentTab === "redaksjonelt-innhold" ? { tittel: "Informasjon for veilederene" } : undefined,
    currentTab === "utbetalinger" ? { tittel: "Utbetalinger" } : undefined,
    currentTab === "deltakerliste" ? { tittel: "Deltakerliste" } : undefined,
  ];

  return (
    <>
      <title>{`Gjennomføring | ${detaljer.gjennomforing.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner
        ikon={
          isGjennomforingAvtaleDetaljer(detaljer) ? (
            <GjennomforingAvtaleIkon />
          ) : (
            <GjennomforingEnkeltplassIkon />
          )
        }
        heading={detaljer.gjennomforing.navn}
        status={detaljer.gjennomforing.status.status}
      />
      {"deltaker" in detaljer && detaljer.deltaker && (
        <GjennomforingEnkeltplassHeader
          gjennomforing={detaljer.gjennomforing}
          deltaker={detaljer.deltaker}
          short
        />
      )}
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
            <Tabs.Panel value={currentTab} data-testid="gjennomforing_info-container">
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

const ENKELTPLASS_ANSKAFFET_TABS: TabConfig[] = [
  { key: "detaljer", label: "Detaljer" },
  { key: "tilsagn", label: "Tilsagn" },
  { key: "utbetalinger", label: "Utbetalinger" },
];

const ENKELTPLASS_TILSKUDD_TABS: TabConfig[] = [
  { key: "detaljer", label: "Detaljer" },
  {
    key: "tilskudd-behandling",
    label: "Tilskuddsbehandlinger",
  },
  { key: "tilskudd-utbetalinger", label: "Utbetalinger" },
];

const ENKELTPLASS_INGEN_KOSTNADER_TABS: TabConfig[] = [{ key: "detaljer", label: "Detaljer" }];

const TAB_KEYS = [
  "tilskudd-behandling",
  "tilsagn",
  "redaksjonelt-innhold",
  "deltakerliste",
  "utbetalinger",
  "tilskudd-utbetalinger",
] as const;

function createTabUrl(gjennomforingId: string, tabKey: string): string {
  return tabKey === "detaljer"
    ? `/gjennomforinger/${gjennomforingId}`
    : `/gjennomforinger/${gjennomforingId}/${tabKey}`;
}

function getCurrentTab(pathname: string): string {
  const tabKey = [...TAB_KEYS]
    .sort((a, b) => b.length - a.length)
    .find((key) => pathname.includes(key));
  return tabKey || "detaljer";
}

function enkeltplassTabs(
  prismodell: PrismodellType,
  okonomi: TotrinnskontrollDto | null,
): TabConfig[] {
  // Ingen tabs før økonomi er godkjent
  if (!okonomi || !erGodkjent(okonomi)) {
    return ENKELTPLASS_INGEN_KOSTNADER_TABS;
  }
  switch (prismodell) {
    case PrismodellType.TILSKUDD_TIL_OPPLAERING:
      return ENKELTPLASS_TILSKUDD_TABS;
    case PrismodellType.INGEN_KOSTNADER:
      return ENKELTPLASS_INGEN_KOSTNADER_TABS;
    case PrismodellType.ANSKAFFET_ENKELTPLASS:
      return ENKELTPLASS_ANSKAFFET_TABS;
    case PrismodellType.ANNEN_AVTALT_PRIS:
    case PrismodellType.FAST_SATS_PER_BENYTTET_PLASS_PER_MANED:
    case PrismodellType.FAST_SATS_PER_AVTALT_PLASS_PER_MANED:
    case PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_MANED:
    case PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_UKE:
    case PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_HELE_UKE:
    case PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER:
      return [];
  }
}

function useTabs(
  detaljer: GjennomforingDetaljerDto,
  enableTilskuddsbehandling: boolean,
): [string, Tab[]] {
  const { pathname } = useLocation();
  const currentTab = getCurrentTab(pathname);
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();

  const tabConfigs = isGjennomforingAvtaleDetaljer(detaljer)
    ? GRUPPETILTAK_TABS
    : enkeltplassTabs(detaljer.prismodell.type, detaljer.okonomi);

  const filteredTabConfigs = enableTilskuddsbehandling
    ? tabConfigs
    : tabConfigs.filter((tab) => tab.key !== "tilskudd-behandling");

  const tabs: Tab[] = filteredTabConfigs.map(({ key, label }) => ({
    key,
    label,
    onClick: () => navigateAndReplaceUrl(createTabUrl(detaljer.gjennomforing.id, key)),
  }));

  return [currentTab, tabs];
}
