import { Tabs } from "@navikt/ds-react";
import { ErrorBoundary } from "react-error-boundary";
import { useAvtale } from "../../../api/avtaler/useAvtale";
import { ErrorFallback } from "../../../main";
import { NavLink, Outlet, useLocation } from "react-router-dom";

export function TiltaksgjennomforingerForAvtale() {
  const { data: avtale } = useAvtale();
  const { pathname } = useLocation();

  return (
    <>
      <Tabs value={ pathname.includes("utkast") ? "utkast" : "gjennomforinger"} >
        <Tabs.List>
          <NavLink to={`/avtaler/${avtale?.id}/tiltaksgjennomforinger`} >
            <Tabs.Tab value="gjennomforinger" label="Gjennomføringer" />
          </NavLink>
          <NavLink to={`/avtaler/${avtale?.id}/tiltaksgjennomforinger/utkast`} >
            <Tabs.Tab
              data-testid="mine-utkast-tab"
              value="utkast"
              label="Mine utkast"
            />
          </NavLink>
        </Tabs.List>
        <ErrorBoundary FallbackComponent={ErrorFallback}>
          <Outlet />
        </ErrorBoundary>
      </Tabs>
    </>
  );
}