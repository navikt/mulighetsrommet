import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { Chips } from "@navikt/ds-react";

interface Props {
  tiltakskoder: Tiltakskode[];
  onRemove: (tiltakskode: Tiltakskode) => void;
}

export function TiltakstypeFilterTags({ tiltakskoder, onRemove }: Props) {
  const tiltakstyper = useTiltakstyper();

  if (tiltakskoder.length === 0) {
    return null;
  }

  return (
    <Chips>
      {tiltakskoder.map((kode) => (
        <Chips.Removable key={kode} onClick={() => onRemove(kode)}>
          {tiltakstyper.find((t) => t.tiltakskode === kode)?.navn || kode}
        </Chips.Removable>
      ))}
    </Chips>
  );
}
