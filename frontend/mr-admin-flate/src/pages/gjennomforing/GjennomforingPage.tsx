import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { Header } from "@/components/detaljside/Header";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Laster } from "@/components/laster/Laster";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { PREVIEW_ARBEIDSMARKEDSTILTAK_URL } from "@/constants";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { GjennomforingOppstartstype, GjennomforingStatus, Toggles } from "@mr/api-client-v2";
import { Lenkeknapp } from "@mr/frontend-common/components/lenkeknapp/Lenkeknapp";
import { Heading, Tabs, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import React from "react";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { GjennomforingStatusMedAarsakTag } from "@/components/statuselementer/GjennomforingStatusMedAarsakTag";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Outlet, useLocation } from "react-router";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";

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
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId);

  const { data: enableTilsagn } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_TILSAGN,
    gjennomforing && [gjennomforing.tiltakstype.tiltakskode],
  );

  const { data: enableOkonomi } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_UTBETALING,
    gjennomforing && [gjennomforing.tiltakstype.tiltakskode],
  );

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
          </div>
          {gjennomforing.status.type === GjennomforingStatus.GJENNOMFORES && (
            <div className="pr-2">
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
            value="detaljer"
            label="Detaljer"
            onClick={() => navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}`)}
          />
          <Tabs.Tab
            value="redaksjonelt-innhold"
            label="Informasjon til veilederene"
            onClick={() =>
              navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/redaksjonelt-innhold`)
            }
          />
          {enableTilsagn ? (
            <Tabs.Tab
              value="tilsagn"
              label="Tilsagn"
              onClick={() => navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/tilsagn`)}
            />
          ) : null}
          {enableOkonomi ? (
            <Tabs.Tab
              value="utbetalinger"
              label="Utbetalinger"
              onClick={() =>
                navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/utbetalinger`)
              }
            />
          ) : null}
          {gjennomforing.oppstart === GjennomforingOppstartstype.FELLES && (
            <Tabs.Tab
              value="deltakerliste"
              label="Deltakerliste"
              onClick={() =>
                navigateAndReplaceUrl(`/gjennomforinger/${gjennomforing.id}/deltakerliste`)
              }
            />
          )}
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
