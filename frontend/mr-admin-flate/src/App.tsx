import { Loader } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { lazy, Suspense } from "react";
import { useHentAnsatt } from "./api/administrator/useHentAdministrator";
import { rolleAtom } from "./api/atoms";
import { hentAnsattsRolle } from "./tilgang/tilgang";

export function App() {
  const optionalAnsatt = useHentAnsatt();
  const [rolleSatt] = useAtom(rolleAtom);

  if (optionalAnsatt.isFetching || !optionalAnsatt.data) {
    return <Loader size="xlarge" />;
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
        <Suspense fallback={<Loader size="xlarge" />}>
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
