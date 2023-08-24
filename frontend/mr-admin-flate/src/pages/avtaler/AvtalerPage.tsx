import { Tabs } from "@navikt/ds-react";
import { ErrorBoundary } from "react-error-boundary";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { MainContainer } from "../../layouts/MainContainer";
import { ErrorFallback } from "../../main";
import styles from "../Page.module.scss";
import { Outlet, useLocation, useNavigate } from "react-router-dom";

export function AvtalerPage() {
  const navigate = useNavigate();
  const location = useLocation();
  return (
    <>
      <HeaderBanner heading="Oversikt over avtaler" harUndermeny />
      <ErrorBoundary FallbackComponent={ErrorFallback}>
        <Tabs value={location.pathname}>
          <div className={styles.header_container_tabs} role="contentinfo">
            <Tabs.List>
              <Tabs.Tab
                data-testid="avtaler-tab"
                value="/avtaler"
                label="Avtaler"
                aria-controls="panel"
                onClick={() => navigate("/avtaler")}
              />
              <Tabs.Tab
                data-testid="mine-utkast-tab"
                value="/avtaler/utkast"
                label="Utkast"
                aria-controls="panel"
                onClick={() => navigate("/avtaler/utkast")}
              />
            </Tabs.List>
          </div>
          <MainContainer>
            <div id="panel">
              <Outlet />
            </div>
          </MainContainer>
        </Tabs>
      </ErrorBoundary>
    </>
  );
}
