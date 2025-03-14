import { Tabs } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Outlet, useLocation, useNavigate } from "react-router";
import { lesteNotifikasjonerQuery, ulesteNotifikasjonerQuery } from "./notifikasjonerQueries";

export function NotifikasjonerPage() {
  const { pathname } = useLocation();
  const { data: leste } = useQuery({ ...lesteNotifikasjonerQuery });
  const { data: uleste } = useQuery({ ...ulesteNotifikasjonerQuery });
  const navigate = useNavigate();

  return (
    <main>
      <div className="flex justify-end">
        <Tabs value={pathname.includes("tidligere") ? "tidligere" : "nye"} selectionFollowsFocus>
          <Tabs.List id="fane_liste" className="flex flex-row justify-between">
            <Tabs.Tab
              value="nye"
              label={`Nye notifikasjoner ${uleste?.data.pagination.totalCount ? `(${uleste?.data.pagination.totalCount})` : ""}`}
              onClick={() => navigate("/arbeidsbenk/notifikasjoner")}
              aria-controls="panel"
            />
            <Tabs.Tab
              value="tidligere"
              label={`Tidligere notifikasjoner ${leste?.data.pagination.totalCount ? `(${leste?.data.pagination.totalCount})` : ""}`}
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
