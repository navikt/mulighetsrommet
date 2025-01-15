import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { Header } from "@/components/detaljside/Header";
import headerStyles from "@/components/detaljside/Header.module.scss";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { DupliserGjennomforing } from "@/components/gjennomforing/DupliserGjennomforing";
import { PREVIEW_ARBEIDSMARKEDSTILTAK_URL } from "@/constants";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { Toggles } from "@mr/api-client";
import { GjennomforingStatusMedAarsakTag } from "@mr/frontend-common";
import { Lenkeknapp } from "@mr/frontend-common/components/lenkeknapp/Lenkeknapp";
import { gjennomforingIsAktiv } from "@mr/frontend-common/utils/utils";
import { Heading, Tabs, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import { Outlet, useLoaderData, useLocation } from "react-router";
import commonStyles from "../Page.module.scss";
import { gjennomforingLoader } from "./gjennomforingLoaders";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import React from "react";
import { Laster } from "@/components/laster/Laster";

function createBrodsmuler(
  gjennomforingId: string,
  avtaleId?: string,
  tilsagn?: boolean,
  refusjonskrav?: boolean,
): Array<Brodsmule | undefined> {
  return [
    avtaleId
      ? { tittel: "Avtaler", lenke: "/avtaler" }
      : { tittel: "Gjennomføringer", lenke: "/gjennomforinger" },
    avtaleId ? { tittel: "Avtale", lenke: `/avtaler/${avtaleId}` } : undefined,
    avtaleId
      ? {
          tittel: "Gjennomføringer",
          lenke: `/avtaler/${avtaleId}/gjennomforinger`,
        }
      : undefined,
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforingId}`,
    },
    tilsagn
      ? { tittel: "Tilsagn", lenke: `/gjennomforinger/${gjennomforingId}/tilsagn` }
      : undefined,
    refusjonskrav
      ? {
          tittel: "Refusjonskrav",
          lenke: `/gjennomforinger/${gjennomforingId}/refusjonskrav`,
        }
      : undefined,
  ];
}

export function GjennomforingPage() {
  const { pathname } = useLocation();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();
  const { gjennomforing } = useLoaderData<typeof gjennomforingLoader>();
  const { data: enableOkonomi } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_OKONOMI,
    [gjennomforing.tiltakstype.tiltakskode],
  );

  const currentTab = () => {
    if (pathname.includes("tilsagn")) {
      return "tilsagn";
    } else if (pathname.includes("deltakere")) {
      return "poc";
    } else if (pathname.includes("refusjonskrav")) {
      return "refusjonskrav";
    } else {
      return "gjennomforing";
    }
  };

  const brodsmuler = createBrodsmuler(
    gjennomforing.id,
    gjennomforing.avtaleId,
    currentTab() === "tilsagn",
    currentTab() === "refusjonskrav",
  );

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header harForhandsvisningsknapp>
        <div
          className={classNames(
            headerStyles.header_outer_container,
            headerStyles.header_outer_container_forhandsvisningsknapp,
          )}
        >
          <div className={headerStyles.tiltaksnavn_status}>
            <GjennomforingIkon />
            <VStack>
              <Heading className={headerStyles.navn} size="large" level="2">
                {gjennomforing.navn}
              </Heading>
            </VStack>
            <GjennomforingStatusMedAarsakTag status={gjennomforing.status} />
            <DupliserGjennomforing gjennomforing={gjennomforing} />
          </div>
          {gjennomforingIsAktiv(gjennomforing.status.status) && (
            <div className={headerStyles.forhandsvisningsknapp}>
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

      <Tabs value={currentTab()}>
        <Tabs.List className={commonStyles.list}>
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
