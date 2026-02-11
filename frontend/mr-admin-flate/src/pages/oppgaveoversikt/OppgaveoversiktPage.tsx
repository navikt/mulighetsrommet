import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { Box, Tabs } from "@navikt/ds-react";
import { Outlet, useLocation, useNavigate } from "react-router";
import { OppgaveoversiktIkon } from "@/components/ikoner/OppgaveoversiktIkon";
import { useNotificationSummary } from "@/api/notifikasjoner/useNotifications";

export function OppgaveoversiktPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();

  const summary = useNotificationSummary();
  const { unreadCount } = summary.data;

  const currentTab = pathname.split("/").pop() || "oppgaver";

  return (
    <>
      <title>Oppgaveoversikt</title>
      <HeaderBanner heading="Oppgaveoversikt" ikon={<OppgaveoversiktIkon />} />
      <Box background="default">
        <Tabs value={currentTab} selectionFollowsFocus>
          <Tabs.List id="fane_liste">
            <Tabs.Tab
              value="oppgaver"
              label={`Oppgaver`}
              onClick={() => navigate("/oppgaveoversikt/oppgaver")}
              aria-controls="panel"
            />
            <Tabs.Tab
              value="notifikasjoner"
              label={unreadCount ? `Notifikasjoner (${unreadCount})` : "Notifikasjoner"}
              onClick={() => navigate("/oppgaveoversikt/notifikasjoner")}
              aria-controls="panel"
              data-testid="notifikasjoner"
            />
            <Tabs.Tab
              value="tidligere-notifikasjoner"
              label={"Tidligere notifikasjoner"}
              onClick={() => navigate("/oppgaveoversikt/tidligere-notifikasjoner")}
              aria-controls="panel"
              data-testid="tidligere-notifikasjoner"
            />
          </Tabs.List>
        </Tabs>
      </Box>
      <ContentBox>
        <Outlet />
      </ContentBox>
    </>
  );
}
