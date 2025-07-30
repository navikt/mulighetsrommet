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
import { RedaksjoneltInnholdPreview } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { GjennomforingDetaljer } from "./GjennomforingDetaljer";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { useAtom } from "jotai";
import { DeltakerlisteContainer } from "./deltakerliste/DeltakerlisteContainer";
import { TilsagnForGjennomforingPage } from "./tilsagn/TilsagnForGjennomforingPage";
import { UtbetalingerForGjennomforingContainer } from "./utbetaling/UtbetalingerForGjennomforingContainer";

export function GjennomforingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId);

  const [activeTab, setActiveTab] = useAtom(gjennomforingDetaljerTabAtom);

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
      lenke: activeTab === "detaljer" ? undefined : `/gjennomforinger/${gjennomforing.id}`,
    },
    activeTab === "tilsagn" ? { tittel: "Tilsagnoversikt" } : undefined,
    activeTab === "redaksjonelt-innhold" ? { tittel: "Redaksjonelt innhold" } : undefined,
    activeTab === "utbetalinger" ? { tittel: "Utbetalinger" } : undefined,
    activeTab === "deltakerliste" ? { tittel: "Deltakerliste" } : undefined,
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
      <Tabs value={activeTab}>
        <Tabs.List className="p-[0 0.5rem] w-[1920px] flex items-start m-auto">
          <Tabs.Tab value="detaljer" label="Detaljer" onClick={() => setActiveTab("detaljer")} />
          <Tabs.Tab
            value="redaksjonelt-innhold"
            label="Redaksjonelt innhold"
            onClick={() => setActiveTab("redaksjonelt-innhold")}
          />
          {enableTilsagn ? (
            <Tabs.Tab value="tilsagn" label="Tilsagn" onClick={() => setActiveTab("tilsagn")} />
          ) : null}
          {enableOkonomi ? (
            <Tabs.Tab
              value="utbetalinger"
              label="Utbetalinger"
              onClick={() => setActiveTab("utbetalinger")}
            />
          ) : null}
          {gjennomforing.oppstart === GjennomforingOppstartstype.FELLES && (
            <Tabs.Tab
              value="deltakerliste"
              label="Deltakerliste"
              onClick={() => setActiveTab("deltakerliste")}
            />
          )}
        </Tabs.List>
        <React.Suspense fallback={<Laster tekst="Laster innhold..." />}>
          <ContentBox>
            <WhitePaddedBox>
              <Tabs.Panel value="detaljer" data-testid="gjennomforing_info-container">
                <InlineErrorBoundary>
                  <GjennomforingDetaljer />
                </InlineErrorBoundary>
              </Tabs.Panel>
              <Tabs.Panel value="redaksjonelt-innhold">
                <InlineErrorBoundary>
                  <RedaksjoneltInnholdPreview
                    tiltakstype={gjennomforing.tiltakstype}
                    beskrivelse={gjennomforing.beskrivelse}
                    faneinnhold={gjennomforing.faneinnhold}
                    kontorstruktur={gjennomforing.kontorstruktur}
                  />
                </InlineErrorBoundary>
              </Tabs.Panel>
              <Tabs.Panel value="tilsagn">
                <InlineErrorBoundary>
                  <TilsagnForGjennomforingPage />
                </InlineErrorBoundary>
              </Tabs.Panel>
              <Tabs.Panel value="utbetalinger">
                <InlineErrorBoundary>
                  <UtbetalingerForGjennomforingContainer />
                </InlineErrorBoundary>
              </Tabs.Panel>
              <Tabs.Panel value="deltakerliste">
                <InlineErrorBoundary>
                  <DeltakerlisteContainer />
                </InlineErrorBoundary>
              </Tabs.Panel>
            </WhitePaddedBox>
          </ContentBox>
        </React.Suspense>
      </Tabs>
    </>
  );
}
