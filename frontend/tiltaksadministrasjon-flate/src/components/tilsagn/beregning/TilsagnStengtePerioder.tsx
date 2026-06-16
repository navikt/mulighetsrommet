import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { StengtPeriode } from "@tiltaksadministrasjon/api-client";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";

export interface Props {
  stengt: StengtPeriode[];
}

export function TilsagnStengtePerioder({ stengt }: Props) {
  if (stengt.length === 0) {
    return null;
  }

  return (
    <Definisjonsliste
      title="Stengte perioder"
      definitions={stengt.flatMap(({ periode, beskrivelse }) => [
        { key: "Beskrivelse", value: beskrivelse },
        { key: "Periode", value: formaterPeriode(periode) },
      ])}
    />
  );
}
