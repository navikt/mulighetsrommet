import { Tabs } from "@navikt/ds-react";
import { Outlet, useLocation, useNavigate } from "react-router";
import { useNotificationSummary } from "@/api/notifikasjoner/useNotifications";

export function NotifikasjonerPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();

  const summary = useNotificationSummary();
  const { readCount, unreadCount } = summary.data;

  return (
    <>
      <div className="flex justify-start">
        <Tabs value={pathname.includes("tidligere") ? "tidligere" : "nye"} selectionFollowsFocus>
          <Tabs.List id="fane_liste" className="flex flex-row justify-between">
            <Tabs.Tab
              value="nye"
              label={`Nye notifikasjoner (${unreadCount})`}
              onClick={() => navigate("/oppgaveoversikt/notifikasjoner")}
              aria-controls="panel"
            />
            <Tabs.Tab
              value="tidligere"
              label={`Tidligere notifikasjoner (${readCount})`}
              onClick={() => navigate("/oppgaveoversikt/notifikasjoner/tidligere")}
              aria-controls="panel"
            />
          </Tabs.List>
        </Tabs>
      </div>
      <div id="panel">
        <Outlet />
      </div>
    </>
  );
}
