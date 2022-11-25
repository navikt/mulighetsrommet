import { useHentAnsatt } from "./api/administrator/useHentAdministrator";
import { AutentisertApp } from "./AutentisertApp";
import { IkkeAutentisertApp } from "./IkkeAutentisertApp";
import { ansattErTiltaksansvarlig } from "./tilgang/tilgang";

export function App() {
  const optionalAnsatt = useHentAnsatt();

  if (optionalAnsatt.isFetching) return null;

  return ansattErTiltaksansvarlig(optionalAnsatt?.data) ? (
    <AutentisertApp />
  ) : (
    <IkkeAutentisertApp />
  );
}
