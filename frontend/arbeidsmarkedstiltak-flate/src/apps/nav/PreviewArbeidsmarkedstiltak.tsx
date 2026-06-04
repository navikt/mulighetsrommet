import { Navigate, Route, Routes } from "react-router";
import { AppContainer } from "@/layouts/AppContainer";
import { ArbeidsmarkedstiltakHeader } from "@/components/ArbeidsmarkedstiltakHeader";
import { PreviewArbeidsmarkedstiltakDetaljer } from "./views/PreviewArbeidsmarkedstiltakDetaljer";
import { NavArbeidsmarkedstiltakOversikt } from "@/apps/nav/views/NavArbeidsmarkedstiltakOversikt";
import { ArbeidsmarkedstiltakDetaljerSuspense } from "@/components/suspense/ArbeidsmarkedstiltakDetaljerSuspense";
import { Theme } from "@navikt/ds-react";

export function PreviewArbeidsmarkedstiltak() {
  return (
    <Theme theme="light" data-color="accent" hasBackground={false}>
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
    </Theme>
  );
}
