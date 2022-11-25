import { useHentAnsatt } from "./api/administrator/useHentAdministrator";
import { AutentisertApp } from "./AutentisertApp";
import { IkkeAutentisertApp } from "./IkkeAutentisertApp";
import { ansattErTiltaksansvarlig } from "./tilgang/tilgang";

export function App() {
  const optionalAnsatt = useHentAnsatt();

  return ansattErTiltaksansvarlig(optionalAnsatt?.data) ? (
    <AutentisertApp />
  ) : (
    <IkkeAutentisertApp />
  );
}
