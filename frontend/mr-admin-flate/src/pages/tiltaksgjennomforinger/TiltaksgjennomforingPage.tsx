import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { Header } from "@/components/detaljside/Header";
import headerStyles from "@/components/detaljside/Header.module.scss";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { DupliserTiltak } from "@/components/tiltaksgjennomforinger/DupliserTiltak";
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
import { tiltaksgjennomforingLoader } from "./tiltaksgjennomforingLoaders";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import React from "react";
import { Laster } from "@/components/laster/Laster";

function createBrodsmuler(
  tiltaksgjennomforingId: string,
  avtaleId?: string,
  tilsagn?: boolean,
  refusjonskrav?: boolean,
): Array<Brodsmule | undefined> {
  return [
    avtaleId
      ? { tittel: "Avtaler", lenke: "/avtaler" }
      : { tittel: "Gjennomføringer", lenke: "/tiltaksgjennomforinger" },
    avtaleId ? { tittel: "Avtale", lenke: `/avtaler/${avtaleId}` } : undefined,
    avtaleId
      ? {
          tittel: "Gjennomføringer",
          lenke: `/avtaler/${avtaleId}/tiltaksgjennomforinger`,
        }
      : undefined,
    {
      tittel: "Gjennomføring",
      lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}`,
    },
    tilsagn
      ? { tittel: "Tilsagn", lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}/tilsagn` }
      : undefined,
    refusjonskrav
      ? {
          tittel: "Refusjonskrav",
          lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}/refusjonskrav`,
        }
      : undefined,
  ];
}

export function TiltaksgjennomforingPage() {
  const { pathname } = useLocation();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();
  const { tiltaksgjennomforing } = useLoaderData<typeof tiltaksgjennomforingLoader>();
  const { data: enableOkonomi } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_OKONOMI,
    [tiltaksgjennomforing.tiltakstype.tiltakskode],
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
    tiltaksgjennomforing.id,
    tiltaksgjennomforing.avtaleId,
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
            <TiltaksgjennomforingIkon />
            <VStack>
              <Heading className={headerStyles.navn} size="large" level="2">
                {tiltaksgjennomforing.navn}
              </Heading>
            </VStack>
            <GjennomforingStatusMedAarsakTag status={tiltaksgjennomforing.status} />
            <DupliserTiltak tiltaksgjennomforing={tiltaksgjennomforing} />
          </div>
          {gjennomforingIsAktiv(tiltaksgjennomforing.status.status) && (
            <div className={headerStyles.forhandsvisningsknapp}>
              <Lenkeknapp
                size="small"
                isExternal={true}
                variant="secondary"
                to={`${PREVIEW_ARBEIDSMARKEDSTILTAK_URL}/tiltak/${tiltaksgjennomforing.id}`}
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
            onClick={() =>
              navigateAndReplaceUrl(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`)
            }
            aria-controls="panel"
          />
          {enableOkonomi ? (
            <>
              <Tabs.Tab
                value="tilsagn"
                label="Tilsagn"
                onClick={() =>
                  navigateAndReplaceUrl(
                    `/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/tilsagn`,
                  )
                }
                aria-controls="panel"
              />
              <Tabs.Tab
                value="refusjonskrav"
                label="Refusjonskrav"
                onClick={() =>
                  navigateAndReplaceUrl(
                    `/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/refusjonskrav`,
                  )
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
