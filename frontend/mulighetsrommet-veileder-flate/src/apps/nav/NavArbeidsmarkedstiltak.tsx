import "@navikt/ds-css";
import { Navigate, Route, Routes } from "react-router-dom";
import { AppContainer } from "@/layouts/AppContainer";
import { ArbeidsmarkedstiltakHeader } from "@/components/ArbeidsmarkedstiltakHeader";
import { NavArbeidsmarkedstiltakOversikt } from "./views/NavArbeidsmarkedstiltakOversikt";
import { NavArbeidsmarkedstiltakDetaljer } from "./views/NavArbeidsmarkedstiltakDetaljer";

export function NavArbeidsmarkedstiltak() {
  return (
    <AppContainer header={<ArbeidsmarkedstiltakHeader href={"/"} />}>
      <Routes>
        <Route path="oversikt" element={<NavArbeidsmarkedstiltakOversikt />} />
        <Route path="tiltak/:id/*" element={<NavArbeidsmarkedstiltakDetaljer />} />
        <Route path="*" element={<Navigate replace to="./oversikt" />} />
      </Routes>
    </AppContainer>
  );
}
