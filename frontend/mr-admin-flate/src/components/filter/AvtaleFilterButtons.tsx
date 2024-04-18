import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";
import style from "./AvtaleFilterButtons.module.scss";
import { Lenkeknapp } from "mulighetsrommet-frontend-common/components/lenkeknapp/Lenkeknapp";

export function AvtaleFilterButtons() {
  return (
    <HarSkrivetilgang ressurs="Avtale">
      <Lenkeknapp
        to="/avtaler/skjema"
        size="small"
        variant="primary"
        className={style.opprett_avtale_knapp}
      >
        Opprett ny avtale
      </Lenkeknapp>
    </HarSkrivetilgang>
  );
}
