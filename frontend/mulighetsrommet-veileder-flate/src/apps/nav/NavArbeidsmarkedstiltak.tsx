import { ArbeidsmarkedstiltakHeader } from "@/components/ArbeidsmarkedstiltakHeader";
import { AppContainer } from "@/layouts/AppContainer";
import { MenuGridIcon } from "@navikt/aksel-icons";
import "@navikt/ds-css";
import { Dropdown, InternalHeader, Spacer } from "@navikt/ds-react";
import { Navigate, Route, Routes } from "react-router-dom";
import { OmArbeidsmarkedstiltak } from "./OmArbeidsmarkedstiltak";
import { NavArbeidsmarkedstiltakDetaljer } from "./views/NavArbeidsmarkedstiltakDetaljer";
import { NavArbeidsmarkedstiltakOversikt } from "./views/NavArbeidsmarkedstiltakOversikt";
import { ArbeidsmarkedstiltakDetaljerSuspense } from "@/components/suspense/ArbeidsmarkedstiltakDetaljerSuspense";

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
        <Route
          path="tiltak/:id/*"
          element={
            <ArbeidsmarkedstiltakDetaljerSuspense>
              <NavArbeidsmarkedstiltakDetaljer />
            </ArbeidsmarkedstiltakDetaljerSuspense>
          }
        />
        <Route path="om" element={<OmArbeidsmarkedstiltak />} />
        <Route path="*" element={<Navigate replace to="./oversikt" />} />
      </Routes>
    </AppContainer>
  );
}

function AppHeaderMeny() {
  return (
    <>
      <Spacer />
      <Dropdown>
        <InternalHeader.Button as={Dropdown.Toggle}>
          <MenuGridIcon style={{ fontSize: "1.5rem" }} title="Lenker" />
        </InternalHeader.Button>
        <Dropdown.Menu>
          <Dropdown.Menu.GroupedList>
            <Dropdown.Menu.GroupedList.Item as="a" href="om">
              Om løsningen
            </Dropdown.Menu.GroupedList.Item>
          </Dropdown.Menu.GroupedList>
        </Dropdown.Menu>
      </Dropdown>
    </>
  );
}
