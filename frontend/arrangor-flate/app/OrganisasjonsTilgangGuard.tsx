import { ReactNode } from "react";
import { useOrganisasjonsTilganger } from "./hooks/useOrganisasjonsTilganger";
import IngenTilgang from "./routes/ingen-tilgang";

interface OrganisasjonsTilgangGuardProps {
  children: ReactNode;
}

export default function OrganisasjonsTilgangGuard({ children }: OrganisasjonsTilgangGuardProps) {
  const { data: organisasjonsTilganger } = useOrganisasjonsTilganger();
  if (!organisasjonsTilganger.length) {
    return <IngenTilgang />;
  }
  return <>{children}</>;
}
