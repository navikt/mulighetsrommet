import { Alert, Tabs } from "@navikt/ds-react";
import { Toggles } from "mulighetsrommet-api-client";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import { TiltaksgjennomforingstatusTag } from "../../components/statuselementer/TiltaksgjennomforingstatusTag";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import commonStyles from "../Page.module.scss";
import { erProdMiljo } from "../../utils/Utils";
import { Lenkeknapp } from "../../components/lenkeknapp/Lenkeknapp";

export function TiltaksgjennomforingPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { data: tiltaksgjennomforing, isLoading } = useTiltaksgjennomforingById();
  const forhandsvisningMiljo = import.meta.env.dev || erProdMiljo ? "nav.no" : "dev.nav.no";

  const { data: visDeltakerlisteFraKometFeature } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_VIS_DELTAKERLISTE_FRA_KOMET,
  );
  const { data: showNotater } = useFeatureToggle(Toggles.MULIGHETSROMMET_ADMIN_FLATE_SHOW_NOTATER);

  if (!tiltaksgjennomforing && isLoading) {
    return <Laster tekst="Laster tiltaksgjennomføring" />;
  }

  if (!tiltaksgjennomforing) {
    return (
      <Alert variant="warning">
        Klarte ikke finne tiltaksgjennømforing
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
      <Header>
        <div className={commonStyles.header_outer_container}>
          <div className={commonStyles.header}>
            <span>{tiltaksgjennomforing?.navn ?? "..."}</span>
            <TiltaksgjennomforingstatusTag tiltaksgjennomforing={tiltaksgjennomforing} />
          </div>
          {tiltaksgjennomforing?.sanityId && (
            <div>
              <Lenkeknapp
                size="small"
                isExternal={true}
                variant="secondary"
                to={`https://mulighetsrommet-veileder-flate.intern.${forhandsvisningMiljo}/preview/${tiltaksgjennomforing.id}`}
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
            onClick={() => navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`)}
            aria-controls="panel"
          />
          {showNotater && (
            <Tabs.Tab
              value="notater"
              label="Notater"
              onClick={() => navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/notater`)}
              aria-controls="panel"
            />
          )}
          {visDeltakerlisteFraKometFeature ? (
            <Tabs.Tab
              value="poc"
              label="Deltakerliste"
              onClick={() =>
                navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/deltakere`)
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
