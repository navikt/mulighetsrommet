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
    <Box data-testid="oppgaveoversikt-container">
      <title>Oppgaveoversikt</title>
      <HeaderBanner heading="Oppgaveoversikt" ikon={<OppgaveoversiktIkon />} />
      <Tabs value={currentTab} selectionFollowsFocus>
        <Box background="default">
          <Tabs.List id="fane_liste">
            <Tabs.Tab
              value="oppgaver"
              label={`Oppgaver`}
              onClick={() => navigate("/oppgaveoversikt/oppgaver")}
            />
            <Tabs.Tab
              value="notifikasjoner"
              label={unreadCount ? `Notifikasjoner (${unreadCount})` : "Notifikasjoner"}
              onClick={() => navigate("/oppgaveoversikt/notifikasjoner")}
              data-testid="notifikasjoner"
            />
            <Tabs.Tab
              value="tidligere-notifikasjoner"
              label={"Tidligere notifikasjoner"}
              onClick={() => navigate("/oppgaveoversikt/tidligere-notifikasjoner")}
              data-testid="tidligere-notifikasjoner"
            />
          </Tabs.List>
        </Box>
        <ContentBox>
          <Tabs.Panel value={currentTab}>
            <Outlet />
          </Tabs.Panel>
        </ContentBox>
      </Tabs>
    </Box>
  );
}
