import { Tabs } from "@navikt/ds-react";
import { ErrorBoundary } from "react-error-boundary";
import { useAvtale } from "../../../api/avtaler/useAvtale";
import { ErrorFallback } from "../../../main";
import { Outlet, useLocation, useNavigate } from "react-router-dom";

export function TiltaksgjennomforingerForAvtale() {
  const { data: avtale } = useAvtale();
  const { pathname } = useLocation();
  const navigate = useNavigate();

  return (
    <>
      <Tabs value={pathname.includes("utkast") ? "utkast" : "gjennomforinger"} >
        <Tabs.List>
          <Tabs.Tab
            value="gjennomforinger"
            label="Gjennomføringer"
            onClick={() => navigate(`/avtaler/${avtale?.id}/tiltaksgjennomforinger`)}
            aria-controls="inner-panel"
          />
          <Tabs.Tab
            value="utkast"
            label="Mine utkast"
            onClick={() => navigate(`/avtaler/${avtale?.id}/tiltaksgjennomforinger/utkast`)}
            aria-controls="inner-panel"
            data-testid="mine-utkast-tab"
          />
        </Tabs.List>
        <ErrorBoundary FallbackComponent={ErrorFallback}>
          <div id="inner-panel">
            <Outlet />
          </div>
        </ErrorBoundary>
      </Tabs >
    </>
  );
}
