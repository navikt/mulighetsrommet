import { useAtom } from "jotai";
import { lazy, Suspense } from "react";
import { useHentAnsatt } from "./api/administrator/useHentAdministrator";
import { rolleAtom } from "./api/atoms";
import { Laster } from "./components/Laster";
import { hentAnsattsRolle } from "./tilgang/tilgang";
import { Alert, BodyShort } from "@navikt/ds-react";
import { Forside } from "./Forside";
import { Routes, Route } from "react-router-dom";
import { RootLayout } from "./layouts/RootLayout";
import { ErrorPage } from "./pages/ErrorPage";
import { TiltakstyperPage } from "./pages/tiltakstyper/TiltakstyperPage";
import AutentisertFagansvarligApp from "./AutentisertFagansvarligApp";

export function App() {
  const optionalAnsatt = useHentAnsatt();
  const [rolleSatt] = useAtom(rolleAtom);

  if (optionalAnsatt.error) {
    return (
      <main>
        <Alert variant="error">
          <BodyShort>
            Vi klarte ikke hente brukerinformasjon. Prøv igjen senere.
          </BodyShort>
          <pre>{JSON.stringify(optionalAnsatt?.error, null, 2)}</pre>
        </Alert>
      </main>
    );
  }

  if (optionalAnsatt.isFetching || !optionalAnsatt.data) {
    return (
      <main>
        <Laster tekst="Laster..." size="xlarge" />
      </main>
    );
  }

  const AutentisertTiltaksansvarligApp = lazy(
    () => import("./AutentisertTiltaksansvarligApp")
  );
  // const AutentisertFagansvarligApp = lazy(
  //   () => import("./AutentisertFagansvarligApp")
  // );
  const IkkeAutentisertApp = lazy(() => import("./IkkeAutentisertApp"));

  return (
    <Routes>
      <Route index element={<Forside />} />
      <Route path="/tiltakstyper/*" element={<AutentisertFagansvarligApp />} />
    </Routes>
  );

  // switch (rolleSatt || hentAnsattsRolle(optionalAnsatt.data)) {
  //   case "TILTAKSANSVARLIG":
  //     return (
  // <Suspense
  //   fallback={<Laster tekst="Laster applikasjon" size="xlarge" />}
  // >
  //   <AutentisertTiltaksansvarligApp />
  // </Suspense>
  //     );
  //   case "FAGANSVARLIG":
  //     return (
  //       <Suspense
  //         fallback={<Laster tekst="Laster applikasjon" size="xlarge" />}
  //       >
  //         <AutentisertFagansvarligApp />
  //       </Suspense>
  //     );
  //   default:
  //     return (
  //       <Suspense
  //         fallback={<Laster tekst="Laster applikasjon" size="xlarge" />}
  //       >
  //         <IkkeAutentisertApp />
  //       </Suspense>
  //     );
  // }
}
