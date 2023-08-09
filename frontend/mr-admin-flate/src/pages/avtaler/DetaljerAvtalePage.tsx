import { Alert, Tabs } from "@navikt/ds-react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import { AvtalestatusTag } from "../../components/statuselementer/AvtalestatusTag";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import commonStyles from "../Page.module.scss";
import { ContainerLayoutDetaljer } from "../../layouts/ContainerLayout";

export function DetaljerAvtalePage() {
  const avtaleId = useGetAvtaleIdFromUrl();
  const location = useLocation();
  if (!avtaleId) {
    throw new Error("Fant ingen avtaleId i url");
  }
  const { data: avtale, isLoading } = useAvtale();

  if (!avtale && isLoading) {
    return (
      <main>
        <Laster tekst="Laster avtale" />
      </main>
    );
  }

  if (!avtale) {
    return (
      <Alert variant="warning">
        Klarte ikke finne avtale
        <div>
          <Link to="/">Til forside</Link>
        </div>
      </Alert>
    );
  }

  const currentTab = () => {
      if (location.pathname.includes("notater")) {
        return "notater";
      } else if (location.pathname.includes("tiltaksgjennomforinger")) {
        return "tiltaksgjennomforinger";
      } else {
        return "info"
      }
  }

  return (
    <main>
      <Header>
        <div className={commonStyles.header}>
          <span>{avtale?.navn ?? "..."}</span>
          <AvtalestatusTag avtale={avtale} />
        </div>
      </Header>
      <Tabs value={currentTab()} >
        <Tabs.List className={commonStyles.list}>
          <NavLink to={`/avtaler/${avtaleId}`} >
            <Tabs.Tab value="info" label="Avtaleinfo" />
          </NavLink>
          <NavLink to={`/avtaler/${avtaleId}/notater`} >
            <Tabs.Tab
              value="notater"
              label="Notater"
              data-testid="tab_avtalenotater"
            />
          </NavLink>
          <NavLink to={`/avtaler/${avtaleId}/tiltaksgjennomforinger`} >
            <Tabs.Tab
              data-testid="avtale-tiltaksgjennomforing-tab"
              value="tiltaksgjennomforinger"
              label="Gjennomføringer"
            />
          </NavLink>
        </Tabs.List>
        <ContainerLayoutDetaljer>
          <Outlet />
        </ContainerLayoutDetaljer>
      </Tabs>
    </main>
  );
}
