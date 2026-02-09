import { HarTilgang } from "@/components/auth/HarTilgang";
import { Button } from "@navikt/ds-react";
import { Rolle } from "@tiltaksadministrasjon/api-client";
import { Link } from "react-router";

export function AvtaleFilterButtons() {
  return (
    <HarTilgang rolle={Rolle.AVTALER_SKRIV}>
      <Button as={Link} to="/avtaler/opprett-avtale" size="small" variant="primary">
        Opprett ny avtale
      </Button>
    </HarTilgang>
  );
}
