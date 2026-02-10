import { Box } from "@navikt/ds-react";
import { VeilederflateTiltak } from "@api-client";

interface Props {
  tiltak: VeilederflateTiltak;
}

export function SidemenyKanKombineresMed({ tiltak }: Props) {
  const { tiltakstype } = tiltak;

  return (
    <Box padding="space-20" background="neutral-soft" id="sidemeny">
      <ul className="list-disc list-inside">
        {tiltakstype.kanKombineresMed.sort().map((tiltakstypen) => (
          <li key={tiltakstypen}>{tiltakstypen}</li>
        ))}
      </ul>
    </Box>
  );
}
