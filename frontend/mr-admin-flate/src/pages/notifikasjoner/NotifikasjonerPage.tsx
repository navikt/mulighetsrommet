import { Tabs } from "@navikt/ds-react";
import { kebabCase } from "mulighetsrommet-veileder-flate/src/utils/Utils";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { HeaderBanner } from "../../layouts/HeaderBanner";

export function NotifikasjonerPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();

  return (
    <main>
      <ContainerLayout>
        <HeaderBanner heading="Notifikasjoner" harUndermeny />
        <Tabs value={pathname.includes("tidligere") ? "tidligere" : "nye"} selectionFollowsFocus>
          <Tabs.List id="fane_liste">
            <Tabs.Tab
              value="nye"
              label="Nye notifikasjoner"
              data-testid={`fane_${kebabCase("Nye notifikasjoner")}`}
              onClick={() => navigate("/notifikasjoner")}
              aria-controls="panel"
            />
            <Tabs.Tab
              value="tidligere"
              label="Tidligere notifikasjoner"
              data-testid={`fane_${kebabCase("Tidligere notifikasjoner")}`}
              onClick={() => navigate("/notifikasjoner/tidligere")}
              aria-controls="panel"
            />
          </Tabs.List>
          <div id="panel">
            <Outlet />
          </div>
        </Tabs>
      </ContainerLayout>
    </main>
  );
}
