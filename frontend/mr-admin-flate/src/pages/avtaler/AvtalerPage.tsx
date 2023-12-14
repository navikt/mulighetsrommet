import { Tabs } from "@navikt/ds-react";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import styles from "../Page.module.scss";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import { useTitle } from "mulighetsrommet-frontend-common";
import { ReloadAppErrorBoundary } from "../../ErrorBoundary";

export function AvtalerPage() {
  const navigate = useNavigate();
  const location = useLocation();
  useTitle("Avtaler");

  return (
    <>
      <HeaderBanner heading="Oversikt over avtaler" harUndermeny />
      <ReloadAppErrorBoundary>
        <Tabs value={location.pathname}>
          <div className={styles.header_container_tabs} role="contentinfo">
            <Tabs.List>
              <Tabs.Tab
                value="/avtaler"
                label="Avtaler"
                aria-controls="panel"
                onClick={() => navigate("/avtaler")}
              />
              <Tabs.Tab
                value="/avtaler/utkast"
                label="Utkast"
                aria-controls="panel"
                onClick={() => navigate("/avtaler/utkast")}
              />
            </Tabs.List>
          </div>
          <MainContainer>
            <ContainerLayout>
              <div id="panel">
                <Outlet />
              </div>
            </ContainerLayout>
          </MainContainer>
        </Tabs>
      </ReloadAppErrorBoundary>
    </>
  );
}
