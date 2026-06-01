import { ArbeidsmarkedstiltakHeader } from "@/components/ArbeidsmarkedstiltakHeader";
import { ArbeidsmarkedstiltakDetaljerSuspense } from "@/components/suspense/ArbeidsmarkedstiltakDetaljerSuspense";
import { AppContainer } from "@/layouts/AppContainer";
import { MenuGridIcon } from "@navikt/aksel-icons";
import { ActionMenu, InternalHeader, Spacer, Theme } from "@navikt/ds-react";
import { Navigate, Route, Routes } from "react-router";
import { OmArbeidsmarkedstiltak } from "./OmArbeidsmarkedstiltak";
import { NavArbeidsmarkedstiltakDetaljer } from "./views/NavArbeidsmarkedstiltakDetaljer";
import { NavArbeidsmarkedstiltakOversikt } from "./views/NavArbeidsmarkedstiltakOversikt";

export function NavArbeidsmarkedstiltak() {
  return (
    <AppContainer
      header={
        <ArbeidsmarkedstiltakHeader href={"/"}>
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
  );
}

function AppHeaderMeny() {
  const href = window.location.pathname.startsWith("/nav")
    ? `${window.location.origin}/nav/om`
    : `${window.location.origin}/arbeidsmarkedstiltak/om`;
  return (
    <>
      <Spacer />
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
    </>
  );
}
