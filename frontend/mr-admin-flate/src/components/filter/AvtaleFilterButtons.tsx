import { HarTilgang } from "@/components/auth/HarTilgang";
import { Lenkeknapp } from "@mr/frontend-common/components/lenkeknapp/Lenkeknapp";
import { Rolle } from "@tiltaksadministrasjon/api-client";

export function AvtaleFilterButtons() {
  return (
    <HarTilgang rolle={Rolle.AVTALER_SKRIV}>
      <Lenkeknapp to="/avtaler/skjema" size="small" variant="primary">
        Opprett ny avtale
      </Lenkeknapp>
    </HarTilgang>
  );
}
