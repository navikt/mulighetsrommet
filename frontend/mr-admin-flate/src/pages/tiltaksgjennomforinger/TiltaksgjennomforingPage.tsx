import { Alert, Heading, Tabs } from "@navikt/ds-react";
import classNames from "classnames";
import { Toggles } from "mulighetsrommet-api-client";
import { Link, Outlet, useLocation, useParams } from "react-router-dom";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Header } from "../../components/detaljside/Header";
import headerStyles from "../../components/detaljside/Header.module.scss";
import { Laster } from "../../components/laster/Laster";
import { Lenkeknapp } from "../../components/lenkeknapp/Lenkeknapp";
import { TiltaksgjennomforingstatusTag } from "../../components/statuselementer/TiltaksgjennomforingstatusTag";
import { useNavigateAndReplaceUrl } from "../../hooks/useNavigateWithoutReplacingUrl";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import commonStyles from "../Page.module.scss";
import { DupliserTiltak } from "../../components/tiltaksgjennomforinger/DupliserTiltak";
import { PREVIEW_ARBEIDSMARKEDSTILTAK_URL } from "../../constants";
import { Brodsmule, Brodsmuler } from "../../components/navigering/Brodsmuler";
import { TiltaksgjennomforingIkon } from "../../components/ikoner/TiltaksgjennomforingIkon";

function useTiltaksgjennomforingBrodsmuler(
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
  const { data: showNotater } = useFeatureToggle(Toggles.MULIGHETSROMMET_ADMIN_FLATE_SHOW_NOTATER);
  const brodsmuler = useTiltaksgjennomforingBrodsmuler(tiltaksgjennomforing?.id!!, avtaleId);

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
    if (pathname.includes("deltakere")) {
      return "poc";
    } else if (pathname.includes("notater")) {
      return "notater";
    } else {
      return "info";
    }
  };

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
            <Heading className={headerStyles.navn} size="large" level="2">
              {tiltaksgjennomforing?.navn ?? "..."}
            </Heading>
            <TiltaksgjennomforingstatusTag tiltaksgjennomforing={tiltaksgjennomforing} />
            <DupliserTiltak tiltaksgjennomforing={tiltaksgjennomforing} />
          </div>
          {tiltaksgjennomforing?.id && (
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
          {showNotater && (
            <Tabs.Tab
              value="notater"
              label="Notater"
              onClick={() =>
                navigateAndReplaceUrl(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/notater`)
              }
              aria-controls="panel"
            />
          )}
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
