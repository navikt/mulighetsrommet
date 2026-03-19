import { ReactNode } from "react";
import { useOrganisasjonsTilganger } from "./hooks/useOrganisasjonsTilganger";
import IngenTilgang from "./components/IngenTilgang";
import { Laster } from "./components/common/Laster";

interface OrganisasjonsTilgangGuardProps {
  children: ReactNode;
}

export default function OrganisasjonsTilgangGuard({ children }: OrganisasjonsTilgangGuardProps) {
  const { data: organisasjonsTilganger, isLoading } = useOrganisasjonsTilganger();
  if (isLoading) {
    return <Laster tekst="Laster tiltak..." size="xlarge" />;
  }
  if (!organisasjonsTilganger.length) {
    return <IngenTilgang />;
  }
  return <>{children}</>;
}
