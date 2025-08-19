import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";
import { Lenkeknapp } from "@mr/frontend-common/components/lenkeknapp/Lenkeknapp";

export function AvtaleFilterButtons() {
  return (
    <HarSkrivetilgang ressurs="Avtale">
      <Lenkeknapp to="/avtaler/skjema" size="small" variant="primary">
        Opprett ny avtale
      </Lenkeknapp>
    </HarSkrivetilgang>
  );
}
