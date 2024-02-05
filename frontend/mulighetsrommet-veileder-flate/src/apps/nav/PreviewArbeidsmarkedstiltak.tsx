import "@navikt/ds-css";
import { Navigate, Route, Routes } from "react-router-dom";
import { AppContainer } from "@/layouts/AppContainer";
import { ArbeidsmarkedstiltakHeader } from "@/components/ArbeidsmarkedstiltakHeader";
import { PreviewArbeidsmarkedstiltakOversikt } from "./views/PreviewArbeidsmarkedstiltakOversikt";
import { PreviewArbeidsmarkedstiltakDetaljer } from "./views/PreviewArbeidsmarkedstiltakDetaljer";

export function PreviewArbeidsmarkedstiltak() {
  return (
    <AppContainer header={<ArbeidsmarkedstiltakHeader href={"/preview"} />}>
      <Routes>
        <Route path="oversikt" element={<PreviewArbeidsmarkedstiltakOversikt />} />
        <Route path="tiltak/:id/*" element={<PreviewArbeidsmarkedstiltakDetaljer />} />
        <Route path="*" element={<Navigate replace to="./oversikt" />} />
      </Routes>
    </AppContainer>
  );
}
