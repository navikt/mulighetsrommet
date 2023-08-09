import { Tabs } from "@navikt/ds-react";
import { ErrorBoundary } from "react-error-boundary";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { MainContainer } from "../../layouts/MainContainer";
import { ErrorFallback } from "../../main";
import styles from "../Page.module.scss";
import { NavLink, Outlet } from "react-router-dom";

export function AvtalerPage() {
  return (
    <>
      <HeaderBanner heading="Oversikt over avtaler" harUndermeny />
      <ErrorBoundary FallbackComponent={ErrorFallback}>
        <Tabs
          defaultValue="avtaler"
        >
          <div className={styles.header_container_tabs} role="contentinfo">
            <Tabs.List>
              <NavLink to="/avtaler" >
              <Tabs.Tab
                data-testid="avtaler-tab"
                value="avtaler"
                label="Avtaler"
              />
              </NavLink>
              <NavLink to="/avtaler/utkast" >
                <Tabs.Tab
                  data-testid="mine-utkast-tab"
                  value="utkast"
                  label="Utkast"
                />
              </NavLink>
            </Tabs.List>
          </div>
          <MainContainer>
            <Outlet />
          </MainContainer>
        </Tabs>
      </ErrorBoundary>
    </>
  );
}
