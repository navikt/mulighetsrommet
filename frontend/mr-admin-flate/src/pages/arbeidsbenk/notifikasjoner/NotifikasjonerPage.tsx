import { useTitle } from "@mr/frontend-common";
import { Tabs } from "@navikt/ds-react";
import { Outlet, useLoaderData, useLocation, useNavigate } from "react-router";
import { notifikasjonLoader } from "./notifikasjonerLoader";
import { LoaderData } from "@/types/loader";
export function NotifikasjonerPage() {
  const { pathname } = useLocation();
  const { leste, uleste } = useLoaderData<LoaderData<typeof notifikasjonLoader>>();
  const navigate = useNavigate();
  useTitle("Notifikasjoner");

  return (
    <main>
      <div className="flex justify-end">
        <Tabs value={pathname.includes("tidligere") ? "tidligere" : "nye"} selectionFollowsFocus>
          <Tabs.List id="fane_liste" className="flex flex-row justify-between">
            <Tabs.Tab
              value="nye"
              label={`Nye notifikasjoner ${uleste?.pagination.totalCount ? `(${uleste?.pagination.totalCount})` : ""}`}
              onClick={() => navigate("/arbeidsbenk/notifikasjoner")}
              aria-controls="panel"
            />
            <Tabs.Tab
              value="tidligere"
              label={`Tidligere notifikasjoner ${leste?.pagination.totalCount ? `(${leste?.pagination.totalCount})` : ""}`}
              onClick={() => navigate("/arbeidsbenk/notifikasjoner/tidligere")}
              aria-controls="panel"
            />
          </Tabs.List>
        </Tabs>
      </div>
      <div id="panel">
        <Outlet />
      </div>
    </main>
  );
}
