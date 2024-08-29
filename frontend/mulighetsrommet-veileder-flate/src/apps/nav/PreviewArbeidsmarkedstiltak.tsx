import "@navikt/ds-css";
import { Navigate, Route, Routes } from "react-router-dom";
import { AppContainer } from "@/layouts/AppContainer";
import { ArbeidsmarkedstiltakHeader } from "@/components/ArbeidsmarkedstiltakHeader";
import { PreviewArbeidsmarkedstiltakDetaljer } from "./views/PreviewArbeidsmarkedstiltakDetaljer";
import { NavArbeidsmarkedstiltakOversikt } from "@/apps/nav/views/NavArbeidsmarkedstiltakOversikt";
import { ArbeidsmarkedstiltakDetaljerSuspense } from "@/components/suspense/ArbeidsmarkedstiltakDetaljerSuspense";

export function PreviewArbeidsmarkedstiltak() {
  return (
    <AppContainer header={<ArbeidsmarkedstiltakHeader href={"/preview"} />}>
      <Routes>
        <Route path="oversikt" element={<NavArbeidsmarkedstiltakOversikt preview />} />
        <Route
          path="tiltak/:id/*"
          element={
            <ArbeidsmarkedstiltakDetaljerSuspense>
              <PreviewArbeidsmarkedstiltakDetaljer />
            </ArbeidsmarkedstiltakDetaljerSuspense>
          }
        />
        <Route path="*" element={<Navigate replace to="./oversikt" />} />
      </Routes>
    </AppContainer>
  );
}
