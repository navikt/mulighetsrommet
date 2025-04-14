import { ArrFlateUtbetaling } from "api-client";
import { Definisjonsliste } from "../Definisjonsliste";

interface Props {
  utbetaling: ArrFlateUtbetaling;
  className?: string;
  utenTittel?: boolean;
}

export function GenerelleDetaljer({ className, utbetaling, utenTittel }: Props) {
  const { gjennomforing, tiltakstype } = utbetaling;

  return (
    <Definisjonsliste
      className={className}
      title={utenTittel ? "" : "Generelt"}
      definitions={[
        { key: "Tiltaksnavn", value: gjennomforing.navn },
        { key: "Tiltakstype", value: tiltakstype.navn },
      ]}
    />
  );
}
