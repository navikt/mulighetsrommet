import { useInitializeArbeidsmarkedstiltakFilterForBruker } from "@/apps/modia/hooks/useInitializeArbeidsmarkedstiltakFilterForBruker";
import { DemoImageHeader } from "@/components/DemoImageHeader";
import { ArbeidsmarkedstiltakDetaljerSuspense } from "@/components/suspense/ArbeidsmarkedstiltakDetaljerSuspense";
import { AppContainer } from "@/layouts/AppContainer";
import { Navigate, Route, Routes } from "react-router";
import { Landingsside } from "./views/Landingsside";
import { ModiaArbeidsmarkedstiltakDetaljer } from "./views/ModiaArbeidsmarkedstiltakDetaljer";
import { ModiaArbeidsmarkedstiltakOversikt } from "./views/ModiaArbeidsmarkedstiltakOversikt";
import { Theme } from "@navikt/ds-react";
import { AppTheme } from "@/constants";

interface Props {
  theme: AppTheme;
}

export function ModiaArbeidsmarkedstiltak({ theme }: Props) {
  return (
    <Theme theme={theme} data-color="accent" hasBackground={false}>
      <AppContainer header={<DemoImageHeader />}>
        <ModiaArbeidsmarkedstiltakRoutes />
      </AppContainer>
    </Theme>
  );
}

function ModiaArbeidsmarkedstiltakRoutes() {
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
