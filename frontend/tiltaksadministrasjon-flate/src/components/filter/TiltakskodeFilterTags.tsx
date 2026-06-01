import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { Tiltakskode } from "@tiltaksadministrasjon/api-client";
import { Chips } from "@navikt/ds-react";

interface Props {
  tiltakskoder: Tiltakskode[];
  onRemove: (tiltakskode: Tiltakskode) => void;
}

export function TiltakskodeFilterTags({ tiltakskoder, onRemove }: Props) {
  const tiltakstyper = useTiltakstyper();

  if (tiltakskoder.length === 0) {
    return null;
  }

  return (
    <Chips>
      {tiltakskoder.map((tiltakskode) => (
        <Chips.Removable key={tiltakskode} onClick={() => onRemove(tiltakskode)}>
          {tiltakstyper.find((t) => tiltakskode === t.tiltakskode)?.navn || tiltakskode}
        </Chips.Removable>
      ))}
    </Chips>
  );
}
