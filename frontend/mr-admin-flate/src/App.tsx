import { useAtom } from "jotai";
import { lazy, Suspense } from "react";
import { useHentAnsatt } from "./api/administrator/useHentAdministrator";
import { rolleAtom } from "./api/atoms";
import { Laster } from "./components/Laster";
import { hentAnsattsRolle } from "./tilgang/tilgang";

export function App() {
  const optionalAnsatt = useHentAnsatt();
  const [rolleSatt] = useAtom(rolleAtom);

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
  const AutentisertFagansvarligApp = lazy(
    () => import("./AutentisertFagansvarligApp")
  );
  const IkkeAutentisertApp = lazy(() => import("./IkkeAutentisertApp"));

  switch (rolleSatt || hentAnsattsRolle(optionalAnsatt.data)) {
    case "TILTAKSANSVARLIG":
      return (
        <Suspense
          fallback={<Laster tekst="Laster applikasjon" size="xlarge" />}
        >
          <AutentisertTiltaksansvarligApp ansatt={optionalAnsatt.data} />
        </Suspense>
      );
    case "FAGANSVARLIG":
      return (
        <Suspense
          fallback={<Laster tekst="Laster applikasjon" size="xlarge" />}
        >
          <AutentisertFagansvarligApp />
        </Suspense>
      );
    default:
      return (
        <Suspense
          fallback={<Laster tekst="Laster applikasjon" size="xlarge" />}
        >
          <IkkeAutentisertApp />
        </Suspense>
      );
  }
}
