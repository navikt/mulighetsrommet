import "@navikt/ds-css";
import { Navigate, Route, Routes } from "react-router-dom";
import { AppContainerOversiktView } from "../../components/appContainerOversiktView/AppContainerOversiktView";
import { ArbeidsmarkedstiltakHeader } from "../../components/ArbeidsmarkedstiltakHeader";
import { PreviewArbeidsmarkedstiltakOversikt } from "../../views/preview/PreviewArbeidsmarkedstiltakOversikt";
import { PreviewArbeidsmarkedstiltakDetaljer } from "../../views/preview/PreviewArbeidsmarkedstiltakDetaljer";

export function PreviewArbeidsmarkedstiltak() {
  return (
    <AppContainerOversiktView header={<ArbeidsmarkedstiltakHeader />}>
      <Routes>
        <Route path="oversikt" element={<PreviewArbeidsmarkedstiltakOversikt />} />
        <Route path="tiltak/:id/*" element={<PreviewArbeidsmarkedstiltakDetaljer />} />
        <Route path="*" element={<Navigate replace to="./oversikt" />} />
      </Routes>
    </AppContainerOversiktView>
  );
}
