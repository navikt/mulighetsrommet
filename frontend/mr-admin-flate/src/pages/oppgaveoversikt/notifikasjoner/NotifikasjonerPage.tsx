import { Box, Tabs } from "@navikt/ds-react";
import { Outlet, useLocation, useNavigate } from "react-router";
import { useNotificationSummary } from "@/api/notifikasjoner/useNotifications";

export function NotifikasjonerPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();

  const summary = useNotificationSummary();
  const { readCount, unreadCount } = summary.data;

  return (
    <>
      <Box background="default" width="max-content">
        <Tabs value={pathname.includes("tidligere") ? "tidligere" : "nye"} selectionFollowsFocus>
          <Tabs.List id="fane_liste">
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
      </Box>
      <div id="panel">
        <Outlet />
      </div>
    </>
  );
}
