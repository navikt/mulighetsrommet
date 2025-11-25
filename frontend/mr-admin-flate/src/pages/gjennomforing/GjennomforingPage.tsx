import { Header } from "@/components/detaljside/Header";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Laster } from "@/components/laster/Laster";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { PREVIEW_ARBEIDSMARKEDSTILTAK_URL } from "@/constants";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Lenkeknapp } from "@mr/frontend-common/components/lenkeknapp/Lenkeknapp";
import { Heading, HStack, Spacer, Tabs } from "@navikt/ds-react";
import React from "react";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Outlet, useLocation } from "react-router";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { GjennomforingStatusType } from "@tiltaksadministrasjon/api-client";
import { DataElementStatusTag } from "@mr/frontend-common";

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
export function GjennomforingPage() {
  const { pathname } = useLocation();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();
  const currentTab = getCurrentTab(pathname);

  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: gjennomforing } = useGjennomforing(gjennomforingId);

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
        {gjennomforing.status.type === GjennomforingStatusType.GJENNOMFORES && (
          <Lenkeknapp
            size="small"
            isExternal={true}
            variant="secondary"
            to={`${PREVIEW_ARBEIDSMARKEDSTILTAK_URL}/tiltak/${gjennomforing.id}`}
          >
            Forhåndsvis i Modia
          </Lenkeknapp>
        )}
      </Header>
      <Tabs value={currentTab}>
        <Tabs.List className="p-[0 0.5rem] w-[1920px] flex items-start m-auto">
          <Tabs.Tab
            value="detaljer"
            label="Detaljer"
            onClick={() => navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}`)}
          />
          <Tabs.Tab
            value="redaksjonelt-innhold"
            label="Informasjon for veiledere"
            onClick={() =>
              navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/redaksjonelt-innhold`)
            }
          />
          <Tabs.Tab
            value="tilsagn"
            label="Tilsagn"
            onClick={() => navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/tilsagn`)}
          />
          <Tabs.Tab
            value="utbetalinger"
            label="Utbetalinger"
            onClick={() =>
              navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/utbetalinger`)
            }
          />
          <Tabs.Tab
            value="deltakerliste"
            label="Deltakerliste"
            onClick={() =>
              navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/deltakerliste`)
            }
          />
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
