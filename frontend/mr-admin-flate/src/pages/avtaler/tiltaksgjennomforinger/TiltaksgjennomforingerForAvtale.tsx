import { Tabs } from "@navikt/ds-react";
import { useAvtale } from "../../../api/avtaler/useAvtale";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { ReloadAppErrorBoundary } from "../../../ErrorBoundary";

export function TiltaksgjennomforingerForAvtale() {
  const { data: avtale } = useAvtale();
  const { pathname } = useLocation();
  const navigate = useNavigate();

  return (
    <Tabs value={pathname.includes("utkast") ? "utkast" : "gjennomforinger"}>
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
        />
      </Tabs.List>
      <ReloadAppErrorBoundary>
        <div id="inner-panel">
          <Outlet />
        </div>
      </ReloadAppErrorBoundary>
    </Tabs>
  );
}
