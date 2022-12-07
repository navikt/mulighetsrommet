import { useHentAnsatt } from "./api/administrator/useHentAdministrator";
import { AutentisertTiltaksansvarligApp } from "./AutentisertTiltaksansvarligApp";
import { IkkeAutentisertApp } from "./IkkeAutentisertApp";
import { hentAnsattsRolle } from "./tilgang/tilgang";
import { AutentisertFagansvarligApp } from "./AutentisertFagansvarligApp";

export function App() {
  const optionalAnsatt = useHentAnsatt();

  if (optionalAnsatt.isFetching) return null;

  switch (hentAnsattsRolle(optionalAnsatt?.data)) {
    case "TILTAKSANSVARLIG":
      return <AutentisertFagansvarligApp />; //<AutentisertTiltaksansvarligApp />;
    case "FAGANSVARLIG":
      return <AutentisertFagansvarligApp />;
    default:
      return <IkkeAutentisertApp />;
  }
}
