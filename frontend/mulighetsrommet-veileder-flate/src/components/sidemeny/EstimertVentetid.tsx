import { TimerPauseFillIcon } from "@navikt/aksel-icons";
import { BodyShort } from "@navikt/ds-react";
import { EstimertVentetid as EstimertVentetidType } from "@api-client";

interface Props {
  estimertVentetid: EstimertVentetidType;
}

export function EstimertVentetid({ estimertVentetid }: Props) {
  return (
    <BodyShort className="flex items-center gap-2">
      <TimerPauseFillIcon
        className="text-orange-300"
        aria-label="Stoppeklokkeikon for å indikere estimert ventetid for tiltaket"
      />
      Estimert ventetid for tiltaket:{" "}
      {formatertVentetid(estimertVentetid.verdi, estimertVentetid.enhet)}
    </BodyShort>
  );
}

function formatertVentetid(verdi: number, enhet: string): string {
  switch (enhet) {
    case "uke":
      return `${verdi} ${verdi === 1 ? "uke" : "uker"}`;
    case "maned":
      return `${verdi} ${verdi === 1 ? "måned" : "måneder"}`;
    default:
      return "Ukjent enhet for ventetid";
  }
}
