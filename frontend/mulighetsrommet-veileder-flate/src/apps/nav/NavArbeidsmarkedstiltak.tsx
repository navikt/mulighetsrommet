import "@navikt/ds-css";
import { Navigate, Route, Routes } from "react-router-dom";
import { AppContainerOversiktView } from "../../components/appContainerOversiktView/AppContainerOversiktView";
import { ArbeidsmarkedstiltakHeader } from "../../components/ArbeidsmarkedstiltakHeader";
import { NavArbeidsmarkedstiltakOversikt } from "../../views/nav-arbeidsmarkedstiltak/NavArbeidsmarkedstiltakOversikt";
import { NavArbeidsmarkedstiltakDetaljer } from "../../views/nav-arbeidsmarkedstiltak/NavArbeidsmarkedstiltakDetaljer";

export function NavArbeidsmarkedstiltak() {
  return (
    <AppContainerOversiktView header={<ArbeidsmarkedstiltakHeader />}>
      <Routes>
        <Route path="oversikt" element={<NavArbeidsmarkedstiltakOversikt />} />
        <Route path="tiltak/:id/*" element={<NavArbeidsmarkedstiltakDetaljer />} />
        <Route path="*" element={<Navigate replace to="./oversikt" />} />
      </Routes>
    </AppContainerOversiktView>
  );
}
