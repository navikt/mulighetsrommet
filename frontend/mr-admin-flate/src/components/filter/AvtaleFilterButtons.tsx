import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";
import { Lenkeknapp } from "@mr/frontend-common/components/lenkeknapp/Lenkeknapp";
import { avtaleDetaljerTabAtom } from "@/api/atoms";
import { useSetAtom } from "jotai";

export function AvtaleFilterButtons() {
  const setActiveTab = useSetAtom(avtaleDetaljerTabAtom);

  return (
    <HarSkrivetilgang ressurs="Avtale">
      <Lenkeknapp
        to="/avtaler/skjema"
        size="small"
        variant="primary"
        onClick={() => setActiveTab("detaljer")}
      >
        Opprett ny avtale
      </Lenkeknapp>
    </HarSkrivetilgang>
  );
}
