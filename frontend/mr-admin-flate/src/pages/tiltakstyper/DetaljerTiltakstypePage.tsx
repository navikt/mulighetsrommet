import { Alert, Tabs } from "@navikt/ds-react";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import { TiltakstypestatusTag } from "../../components/statuselementer/TiltakstypestatusTag";
import { ContainerLayoutDetaljer } from "../../layouts/ContainerLayout";
import commonStyles from "../Page.module.scss";

export function DetaljerTiltakstypePage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { data: tiltakstype, isLoading } = useTiltakstypeById();

  if (!tiltakstype && isLoading) {
    return <Laster tekst="Laster tiltakstype" />;
  }

  if (!tiltakstype) {
    return (
      <Alert variant="warning">
        Klarte ikke finne tiltakstype
        <div>
          <Link to="/">Til forside</Link>
        </div>
      </Alert>
    );
  }

  return (
    <main>
      <Header>
        <div className={commonStyles.header}>
          <span>{tiltakstype?.navn ?? "..."}</span>
          <TiltakstypestatusTag tiltakstype={tiltakstype} />
        </div>
      </Header>

      <Tabs value={pathname.includes("avtaler") ? "avtaler" : "arenainfo"} >
        <Tabs.List className={commonStyles.list}>
          <Tabs.Tab
            value="arenainfo"
            label="Arenainfo"
            data-testid="tab_arenainfo"
            onClick={() => navigate(`/tiltakstyper/${tiltakstype.id}`)}
            aria-controls="panel"
          />
          <Tabs.Tab
            value="avtaler"
            label="Avtaler"
            data-testid="tab_avtaler"
            onClick={() => navigate(`/tiltakstyper/${tiltakstype.id}/avtaler`)}
            aria-controls="panel"
          />
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
