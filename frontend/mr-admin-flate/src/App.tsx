import { useHentAnsatt } from "./api/administrator/useHentAdministrator";
import { IkkeAutentisertApp } from "./IkkeAutentisertApp";
import { hentAnsattsRolle } from "./tilgang/tilgang";
import { AutentisertFagansvarligApp } from "./AutentisertFagansvarligApp";
import { lazy, Suspense } from "react";

export function App() {
  const optionalAnsatt = useHentAnsatt();

  if (optionalAnsatt.isFetching) return null;

  const AutentisertTiltaksansvarligApp = lazy(
    () => import("./AutentisertTiltaksansvarligApp")
  );
  const AutentisertFagansvarligApp = lazy(
    () => import("./AutentisertFagansvarligApp")
  );
  const IkkeAutentisertApp = lazy(() => import("./IkkeAutentisertApp"));

  switch (hentAnsattsRolle(optionalAnsatt?.data)) {
    case "TILTAKSANSVARLIG":
      return (
        <Suspense fallback={<p>Laster...</p>}>
          {" "}
          <AutentisertTiltaksansvarligApp />
        </Suspense>
      );
    case "FAGANSVARLIG":
      return (
        <Suspense fallback={<p>Laster...</p>}>
          <AutentisertFagansvarligApp />
        </Suspense>
      );
    default:
      return (
        <Suspense fallback={<p>Laster...</p>}>
          <IkkeAutentisertApp />
        </Suspense>
      );
  }
}
