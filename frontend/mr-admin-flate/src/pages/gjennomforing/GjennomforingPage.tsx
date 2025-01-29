import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { Header } from "@/components/detaljside/Header";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { DupliserGjennomforing } from "@/components/gjennomforing/DupliserGjennomforing";
import { PREVIEW_ARBEIDSMARKEDSTILTAK_URL } from "@/constants";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { Toggles } from "@mr/api-client-v2";
import { GjennomforingStatusMedAarsakTag } from "@mr/frontend-common";
import { Lenkeknapp } from "@mr/frontend-common/components/lenkeknapp/Lenkeknapp";
import { gjennomforingIsAktiv } from "@mr/frontend-common/utils/utils";
import { Heading, Tabs, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import { Outlet, useLoaderData, useLocation } from "react-router";
import { gjennomforingLoader } from "./gjennomforingLoaders";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import React from "react";
import { Laster } from "@/components/laster/Laster";

type GjennomforingTab = "tilsagn" | "deltakerliste" | "refusjonskrav" | "gjennomforing";

export function GjennomforingPage() {
  const { pathname } = useLocation();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();
  const { gjennomforing } = useLoaderData<typeof gjennomforingLoader>();

  const { data: enableOkonomi } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_OKONOMI,
    [gjennomforing.tiltakstype.tiltakskode],
  );

  const { data: enableDeltakerliste } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_DELTAKERLISTE,
  );

  function getCurrentTab(): GjennomforingTab {
    if (pathname.includes("tilsagn")) {
      return "tilsagn";
    } else if (pathname.includes("deltakerliste")) {
      return "deltakerliste";
    } else if (pathname.includes("refusjonskrav")) {
      return "refusjonskrav";
    } else {
      return "gjennomforing";
    }
  }

  const currentTab = getCurrentTab();
  const brodsmuler: (Brodsmule | undefined)[] = [
    {
      tittel: "Gjennomføringer",
      lenke: `/gjennomforinger`,
    },
    {
      tittel: "Gjennomføring",
      lenke: currentTab === "gjennomforing" ? undefined : `/gjennomforinger/${gjennomforing.id}`,
    },
    currentTab === "tilsagn" ? { tittel: "Tilsagnoversikt" } : undefined,
    currentTab === "refusjonskrav" ? { tittel: "Refusjonskravoversikt" } : undefined,
    currentTab === "deltakerliste" ? { tittel: "Deltakerliste" } : undefined,
  ];

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <div
          className={classNames("flex justify-between gap-6 flex-wrap w-full [&>span]:self-center")}
        >
          <div className="flex justify-start gap-6 items-center flex-wrap">
            <GjennomforingIkon />
            <VStack>
              <Heading className="max-w-[50rem]" size="large" level="2">
                {gjennomforing.navn}
              </Heading>
            </VStack>
            <GjennomforingStatusMedAarsakTag status={gjennomforing.status} />
            <DupliserGjennomforing gjennomforing={gjennomforing} />
          </div>
          {gjennomforingIsAktiv(gjennomforing.status.status) && (
            <div>
              <Lenkeknapp
                size="small"
                isExternal={true}
                variant="secondary"
                to={`${PREVIEW_ARBEIDSMARKEDSTILTAK_URL}/tiltak/${gjennomforing.id}`}
              >
                Forhåndsvis i Modia
              </Lenkeknapp>
            </div>
          )}
        </div>
      </Header>
      <Tabs value={currentTab}>
        <Tabs.List className="p-[0 0.5rem] w-[1920px] flex items-start m-auto">
          <Tabs.Tab
            value="gjennomforing"
            label="Gjennomføring"
            onClick={() => navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}`)}
            aria-controls="panel"
          />
          {enableOkonomi ? (
            <>
              <Tabs.Tab
                value="tilsagn"
                label="Tilsagn"
                onClick={() =>
                  navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/tilsagn`)
                }
                aria-controls="panel"
              />
              <Tabs.Tab
                value="refusjonskrav"
                label="Refusjonskrav"
                onClick={() =>
                  navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/refusjonskrav`)
                }
                aria-controls="panel"
              />
            </>
          ) : null}
          {enableDeltakerliste && (
            <Tabs.Tab
              value="deltakerliste"
              label="Deltakerliste"
              onClick={() =>
                navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/deltakerliste`)
              }
              aria-controls="panel"
            />
          )}
        </Tabs.List>
        <React.Suspense fallback={<Laster tekst="Laster innhold..." />}>
          <ContentBox>
            <WhitePaddedBox>
              <div id="panel">
                <Outlet />
              </div>
            </WhitePaddedBox>
          </ContentBox>
        </React.Suspense>
      </Tabs>
    </>
  );
}
