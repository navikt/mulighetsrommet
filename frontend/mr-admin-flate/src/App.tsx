import { useHentAnsatt } from "./api/administrator/useHentAdministrator";
import { hentAnsattsRolle } from "./tilgang/tilgang";
import { lazy, Suspense } from "react";
import { Loader } from "@navikt/ds-react";

export function App() {
  const optionalAnsatt = useHentAnsatt();

  if (optionalAnsatt.isFetching) return <Loader size="xlarge" />;

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
        <Suspense fallback={<Loader size="xlarge" />}>
          {" "}
          <AutentisertTiltaksansvarligApp />
        </Suspense>
      );
    case "FAGANSVARLIG":
      return (
        <Suspense fallback={<Loader size="xlarge" />}>
          <AutentisertFagansvarligApp />
        </Suspense>
      );
    default:
      return (
        <Suspense fallback={<Loader size="xlarge" />}>
          <IkkeAutentisertApp />
        </Suspense>
      );
  }
}
