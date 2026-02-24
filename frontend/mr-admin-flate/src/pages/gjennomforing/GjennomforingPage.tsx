import { Header } from "@/components/detaljside/Header";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Laster } from "@/components/laster/Laster";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Heading, Spacer, Tabs } from "@navikt/ds-react";
import React from "react";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Outlet, useLocation } from "react-router";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { GjennomforingDto } from "@tiltaksadministrasjon/api-client";
import { DataElementStatusTag } from "@mr/frontend-common";
import { isGruppetiltak } from "@/api/gjennomforing/utils";

export function GjennomforingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing } = useGjennomforing(gjennomforingId);
  const [currentTab, tabs] = useTabs(gjennomforing);

  const brodsmuler: (Brodsmule | undefined)[] = [
    {
      tittel: "Gjennomføringer",
      lenke: `/gjennomforinger`,
    },
    {
      tittel: "Gjennomføring",
      lenke: currentTab === "detaljer" ? undefined : `/gjennomforinger/${gjennomforing.id}`,
    },
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
        <Spacer />
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

function useTabs(gjennomforing: GjennomforingDto): [string, Tab[]] {
  const { pathname } = useLocation();
  const currentTab = getCurrentTab(pathname);

  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();
  const tabs = isGruppetiltak(gjennomforing)
    ? [
        {
          key: "detaljer",
          label: "Detaljer",
          onClick: () => navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}`),
        },
        {
          key: "redaksjonelt-innhold",
          label: "Informasjon for veiledere",
          onClick: () =>
            navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/redaksjonelt-innhold`),
        },
        {
          key: "tilsagn",
          label: "Tilsagn",
          onClick: () => navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/tilsagn`),
        },
        {
          key: "utbetalinger",
          label: "Utbetalinger",
          onClick: () => navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/utbetalinger`),
        },
        {
          key: "deltakerliste",
          label: "Deltakerliste",
          onClick: () =>
            navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/deltakerliste`),
        },
      ]
    : [
        {
          key: "detaljer",
          label: "Detaljer",
          onClick: () => navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}`),
        },
        {
          key: "tilsagn",
          label: "Tilsagn",
          onClick: () => navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/tilsagn`),
        },
        {
          key: "utbetalinger",
          label: "Utbetalinger",
          onClick: () => navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/utbetalinger`),
        },
      ];

  return [currentTab, tabs];
}

function getCurrentTab(pathname: string) {
  if (pathname.includes("tilsagn")) {
    return "tilsagn";
  } else if (pathname.includes("redaksjonelt-innhold")) {
    return "redaksjonelt-innhold";
  } else if (pathname.includes("deltakerliste")) {
    return "deltakerliste";
  } else if (pathname.includes("utbetalinger")) {
    return "utbetalinger";
  } else {
    return "detaljer";
  }
}
