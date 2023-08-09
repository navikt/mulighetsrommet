import { Alert, Tabs } from "@navikt/ds-react";
import { Toggles } from "mulighetsrommet-api-client";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import { TiltaksgjennomforingstatusTag } from "../../components/statuselementer/TiltaksgjennomforingstatusTag";
import { ContainerLayoutDetaljer } from "../../layouts/ContainerLayout";
import commonStyles from "../Page.module.scss";

export function DetaljerTiltaksgjennomforingerPage() {
  const { pathname } = useLocation();
  const { data: tiltaksgjennomforing, isLoading } = useTiltaksgjennomforingById();

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
  }

  return (
    <main>
      <Header>
        <div className={commonStyles.header}>
          <span>{tiltaksgjennomforing?.navn ?? "..."}</span>
          <TiltaksgjennomforingstatusTag
            tiltaksgjennomforing={tiltaksgjennomforing}
          />
        </div>
      </Header>

      <Tabs value={currentTab()} >
        <Tabs.List className={commonStyles.list}>
          <NavLink to={`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`} >
            <Tabs.Tab
              value="info"
              label="Info"
              data-testid="tab_detaljer"
            />
          </NavLink>
          <NavLink to={`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/notater`} >
            <Tabs.Tab value="notater" label="Notater" />
          </NavLink>

          {visDeltakerlisteFraKometFeature ? (
            <NavLink to={`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/deltakere`} >
              <Tabs.Tab value="poc" label="Deltakerliste" />
            </NavLink>
          ) : null}
        </Tabs.List>
        <ContainerLayoutDetaljer>
          <Outlet />
        </ContainerLayoutDetaljer>
      </Tabs>
    </main>
  );
}
