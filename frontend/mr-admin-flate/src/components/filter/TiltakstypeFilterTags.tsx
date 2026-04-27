import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { Chips } from "@navikt/ds-react";

interface Props {
  ids: string[];
  onRemove: (tiltakstype: string) => void;
}

export function TiltakstypeFilterTags({ ids, onRemove }: Props) {
  const tiltakstyper = useTiltakstyper();

  if (ids.length === 0) {
    return null;
  }

  return (
    <Chips>
      {ids.map((id) => (
        <Chips.Removable key={id} onClick={() => onRemove(id)}>
          {tiltakstyper.find((t) => id === t.id)?.navn || id}
        </Chips.Removable>
      ))}
    </Chips>
  );
}
