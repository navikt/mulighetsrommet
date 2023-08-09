import { Alert, Tabs } from "@navikt/ds-react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import { TiltakstypestatusTag } from "../../components/statuselementer/TiltakstypestatusTag";
import { ContainerLayoutDetaljer } from "../../layouts/ContainerLayout";
import commonStyles from "../Page.module.scss";

export function DetaljerTiltakstypePage() {
  const { pathname } = useLocation();
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
          <NavLink to={`/tiltakstyper/${tiltakstype.id}`} >
            <Tabs.Tab
              value="arenainfo"
              label="Arenainfo"
              data-testid="tab_arenainfo"
            />
          </NavLink>
          <NavLink to={`/tiltakstyper/${tiltakstype.id}/avtaler`} >
            <Tabs.Tab value="avtaler" label="Avtaler" data-testid="tab_avtaler" />
          </NavLink>
        </Tabs.List>
        <ContainerLayoutDetaljer>
          <Outlet />
        </ContainerLayoutDetaljer>
      </Tabs>
    </main>
  );
}
