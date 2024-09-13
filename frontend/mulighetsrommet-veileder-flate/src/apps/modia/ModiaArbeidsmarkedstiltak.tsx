import { useInitializeArbeidsmarkedstiltakFilterForBruker } from "@/apps/modia/hooks/useInitializeArbeidsmarkedstiltakFilterForBruker";
import { useInitializeModiaContext } from "@/apps/modia/hooks/useInitializeModiaContext";
import { DemoImageHeader } from "@/components/DemoImageHeader";
import { ArbeidsmarkedstiltakDetaljerSuspense } from "@/components/suspense/ArbeidsmarkedstiltakDetaljerSuspense";
import { AppContainer } from "@/layouts/AppContainer";
import "@navikt/ds-css";
import { Navigate, Route, Routes } from "react-router-dom";
import "./polyfill";
import { Landingsside } from "./views/Landingsside";
import { ModiaArbeidsmarkedstiltakDetaljer } from "./views/ModiaArbeidsmarkedstiltakDetaljer";
import { ModiaArbeidsmarkedstiltakOversikt } from "./views/ModiaArbeidsmarkedstiltakOversikt";

export function ModiaArbeidsmarkedstiltak() {
  return (
    <AppContainer header={<DemoImageHeader />}>
      <ModiaArbeidsmarkedstiltakRoutes />
    </AppContainer>
  );
}

function ModiaArbeidsmarkedstiltakRoutes() {
  useInitializeModiaContext();

  useInitializeArbeidsmarkedstiltakFilterForBruker();

  return (
    <Routes>
      <Route path="" element={<Landingsside />} />
      <Route path="oversikt" element={<ModiaArbeidsmarkedstiltakOversikt />} />
      <Route
        path="tiltak/:id/*"
        element={
          <ArbeidsmarkedstiltakDetaljerSuspense>
            <ModiaArbeidsmarkedstiltakDetaljer />
          </ArbeidsmarkedstiltakDetaljerSuspense>
        }
      />
      <Route path="*" element={<Navigate replace to="/arbeidsmarkedstiltak" />} />
    </Routes>
  );
}
