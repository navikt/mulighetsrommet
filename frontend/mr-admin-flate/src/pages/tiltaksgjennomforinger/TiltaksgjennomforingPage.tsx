import { Alert, Tabs } from "@navikt/ds-react";
import { Toggles } from "mulighetsrommet-api-client";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import { TiltaksgjennomforingstatusTag } from "../../components/statuselementer/TiltaksgjennomforingstatusTag";
import { ContainerLayoutDetaljer } from "../../layouts/ContainerLayout";
import commonStyles from "../Page.module.scss";
import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { erProdMiljo } from "../../utils/Utils";
import { Lenkeknapp } from "../../components/lenkeknapp/Lenkeknapp";

export function TiltaksgjennomforingPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { data: tiltaksgjennomforing, isLoading } = useTiltaksgjennomforingById();
  const forhandsvisningMiljo = import.meta.env.dev || erProdMiljo() ? "nav.no" : "dev.nav.no";

  const { data: visDeltakerlisteFraKometFeature } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_VIS_DELTAKERLISTE_FRA_KOMET,
  );

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
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
          }}
        >
          <div className={commonStyles.header}>
            <span>{tiltaksgjennomforing?.navn ?? "..."}</span>
            <TiltaksgjennomforingstatusTag tiltaksgjennomforing={tiltaksgjennomforing} />
          </div>
          {tiltaksgjennomforing?.sanityId && (
            <Lenkeknapp
              size="small"
              variant="secondary"
              to={`https://mulighetsrommet-veileder-flate.intern.${forhandsvisningMiljo}/preview/${tiltaksgjennomforing.sanityId}`}
            >
              Forhåndsvis i Modia <ExternalLinkIcon title="Forhåndsvis gjennomføringen i Modia" />
            </Lenkeknapp>
          )}
        </div>
      </Header>

      <Tabs value={currentTab()}>
        <Tabs.List>
          <Tabs.Tab
            value="info"
            label="Info"
            data-testid="tab_detaljer"
            onClick={() => navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`)}
            aria-controls="panel"
          />
          <Tabs.Tab
            value="notater"
            label="Notater"
            onClick={() => navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/notater`)}
            aria-controls="panel"
          />
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
        <ContainerLayoutDetaljer>
          <div id="panel">
            <Outlet />
          </div>
        </ContainerLayoutDetaljer>
      </Tabs>
    </main>
  );
}
