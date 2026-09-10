import { ArbeidsmarkedstiltakHeader } from "@/components/ArbeidsmarkedstiltakHeader";
import { ArbeidsmarkedstiltakDetaljerSuspense } from "@/components/suspense/ArbeidsmarkedstiltakDetaljerSuspense";
import { AppContainer } from "@/layouts/AppContainer";
import { AppTheme } from "@/constants";
import { MenuGridIcon } from "@navikt/aksel-icons";
import { ActionMenu, InternalHeader, Spacer, Switch, Theme } from "@navikt/ds-react";
import { useState } from "react";
import { Navigate, Route, Routes } from "react-router";
import { OmArbeidsmarkedstiltak } from "./OmArbeidsmarkedstiltak";
import { NavArbeidsmarkedstiltakDetaljer } from "./views/NavArbeidsmarkedstiltakDetaljer";
import { NavArbeidsmarkedstiltakOversikt } from "./views/NavArbeidsmarkedstiltakOversikt";

export function NavArbeidsmarkedstiltak() {
  const [theme, setTheme] = useState<AppTheme>("light");

  return (
    <Theme theme={theme} hasBackground={false}>
      <AppContainer
        header={
          <ArbeidsmarkedstiltakHeader href={"/"}>
            <Spacer />
            <DarkModeSwitch theme={theme} setTheme={setTheme} />
            <AppHeaderMeny />
          </ArbeidsmarkedstiltakHeader>
        }
      >
        <Routes>
          <Route path="oversikt" element={<NavArbeidsmarkedstiltakOversikt />} />
          <Route path="om" element={<OmArbeidsmarkedstiltak />} />
          <Route
            path="tiltak/:id"
            element={
              <ArbeidsmarkedstiltakDetaljerSuspense>
                <NavArbeidsmarkedstiltakDetaljer />
              </ArbeidsmarkedstiltakDetaljerSuspense>
            }
          />
          <Route path="*" element={<Navigate replace to="./oversikt" />} />
        </Routes>
      </AppContainer>
    </Theme>
  );
}

interface DarkModeSwitchProps {
  theme: AppTheme;
  setTheme: (theme: AppTheme) => void;
}

function DarkModeSwitch({ theme, setTheme }: DarkModeSwitchProps) {
  return (
    // Mørk modus alltid for denne switchen siden den ligger på header som alltid er svart
    <Theme theme="dark" hasBackground={false}>
      <div className="flex items-center px-4">
        <Switch
          checked={theme === "dark"}
          onChange={(e) => setTheme(e.target.checked ? "dark" : "light")}
        >
          Mørk modus
        </Switch>
      </div>
    </Theme>
  );
}

function AppHeaderMeny() {
  const href = window.location.pathname.startsWith("/nav")
    ? `${window.location.origin}/nav/om`
    : `${window.location.origin}/arbeidsmarkedstiltak/om`;
  return (
    <ActionMenu>
      <ActionMenu.Trigger>
        <InternalHeader.Button>
          <MenuGridIcon fontSize="1.5rem" title="Lenker" />
        </InternalHeader.Button>
      </ActionMenu.Trigger>
      <Theme theme="light">
        <ActionMenu.Content>
          <ActionMenu.Group label="Nav arbeidsmarkedstiltak">
            <ActionMenu.Item as="a" href={href}>
              Om løsningen
            </ActionMenu.Item>
          </ActionMenu.Group>
        </ActionMenu.Content>
      </Theme>
    </ActionMenu>
  );
}
