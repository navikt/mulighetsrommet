import { useTiltaksgjennomforingById } from "@/api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Alert, Heading, Tabs, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import { Lenkeknapp } from "@mr/frontend-common/components/lenkeknapp/Lenkeknapp";
import { Link, Outlet, useLocation, useParams } from "react-router-dom";
import { ShowOpphavValue } from "@/components/debug/ShowOpphavValue";
import { Header } from "@/components/detaljside/Header";
import headerStyles from "@/components/detaljside/Header.module.scss";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { Laster } from "@/components/laster/Laster";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { DupliserTiltak } from "@/components/tiltaksgjennomforinger/DupliserTiltak";
import { PREVIEW_ARBEIDSMARKEDSTILTAK_URL } from "@/constants";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import commonStyles from "../Page.module.scss";
import { TiltaksgjennomforingStatusTag } from "@mr/frontend-common";
import { TiltaksgjennomforingStatus, Toggles } from "@mr/api-client";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";

function createBrodsmuler(
  tiltaksgjennomforingId: string,
  avtaleId?: string,
): Array<Brodsmule | undefined> {
  return [
    { tittel: "Forside", lenke: "/" },
    avtaleId
      ? { tittel: "Avtaler", lenke: "/avtaler" }
      : { tittel: "Tiltaksgjennomføringer", lenke: "/tiltaksgjennomforinger" },
    avtaleId ? { tittel: "Avtaledetaljer", lenke: `/avtaler/${avtaleId}` } : undefined,
    avtaleId
      ? {
          tittel: "Avtalens gjennomføringer",
          lenke: `/avtaler/${avtaleId}/tiltaksgjennomforinger`,
        }
      : undefined,
    {
      tittel: "Tiltaksgjennomføringdetaljer",
      lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}`,
    },
  ];
}

export function TiltaksgjennomforingPage() {
  const { pathname } = useLocation();
  const { avtaleId } = useParams();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();
  const { data: tiltaksgjennomforing, isLoading } = useTiltaksgjennomforingById();
  const { data: enableOpprettTilsagn } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_OPPRETT_TILSAGN,
  );

  if (!tiltaksgjennomforing && isLoading) {
    return <Laster tekst="Laster tiltaksgjennomføring" />;
  }

  if (!tiltaksgjennomforing) {
    return (
      <Alert variant="warning">
        Klarte ikke finne tiltaksgjennomføring
        <div>
          <Link to="/">Til forside</Link>
        </div>
      </Alert>
    );
  }

  const currentTab = () => {
    if (pathname.includes("tilsagn")) {
      return "tilsagn";
    } else if (pathname.includes("deltakere")) {
      return "poc";
    } else {
      return "info";
    }
  };

  const brodsmuler = createBrodsmuler(tiltaksgjennomforing.id, avtaleId);

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header harForhandsvisningsknapp>
        <div
          className={classNames(
            headerStyles.header_outer_container,
            tiltaksgjennomforing?.id
              ? headerStyles.header_outer_container_forhandsvisningsknapp
              : null,
          )}
        >
          <div className={headerStyles.tiltaksnavn_status}>
            <TiltaksgjennomforingIkon />
            <VStack>
              <Heading className={headerStyles.navn} size="large" level="2">
                {tiltaksgjennomforing?.navn ?? "..."}
              </Heading>
              <ShowOpphavValue value={tiltaksgjennomforing?.opphav} />
            </VStack>
            <TiltaksgjennomforingStatusTag status={tiltaksgjennomforing.status} showAvbruttAarsak />
            <DupliserTiltak tiltaksgjennomforing={tiltaksgjennomforing} />
          </div>
          {tiltaksgjennomforing?.id &&
            [TiltaksgjennomforingStatus.GJENNOMFORES, TiltaksgjennomforingStatus.PLANLAGT].includes(
              tiltaksgjennomforing.status.status,
            ) && (
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
            value="info"
            label="Info"
            onClick={() =>
              navigateAndReplaceUrl(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`)
            }
            aria-controls="panel"
          />
          {enableOpprettTilsagn ? (
            <Tabs.Tab
              value="tilsagn"
              label="Tilsagn"
              onClick={() =>
                navigateAndReplaceUrl(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/tilsagn`)
              }
              aria-controls="panel"
            />
          ) : null}
        </Tabs.List>
        <ContainerLayout>
          <div id="panel">
            <Outlet />
          </div>
        </ContainerLayout>
      </Tabs>
    </main>
  );
}
