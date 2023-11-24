import { Alert, Tabs } from "@navikt/ds-react";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import { TiltakstypestatusTag } from "../../components/statuselementer/TiltakstypestatusTag";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import commonStyles from "../Page.module.scss";
import { useTitle } from "mulighetsrommet-frontend-common";

export function DetaljerTiltakstypePage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { data: tiltakstype, isLoading } = useTiltakstypeById();
  useTitle(`Tiltakstyper ${tiltakstype?.navn ? `- ${tiltakstype.navn}` : ""}`);

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

      <Tabs value={pathname.includes("avtaler") ? "avtaler" : "arenainfo"}>
        <Tabs.List className={commonStyles.list}>
          <Tabs.Tab
            value="arenainfo"
            label="Arenainfo"
            onClick={() => navigate(`/tiltakstyper/${tiltakstype.id}`)}
            aria-controls="panel"
          />
          <Tabs.Tab
            value="avtaler"
            label="Avtaler"
            onClick={() => navigate(`/tiltakstyper/${tiltakstype.id}/avtaler`)}
            aria-controls="panel"
          />
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
