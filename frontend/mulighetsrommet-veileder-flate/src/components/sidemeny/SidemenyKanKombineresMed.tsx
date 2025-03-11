import { Box } from "@navikt/ds-react";
import { VeilederflateTiltak } from "@mr/api-client-v2";

interface Props {
  tiltak: VeilederflateTiltak;
}

export function SidemenyKanKombineresMed({ tiltak }: Props) {
  const { tiltakstype } = tiltak;

  return (
    <Box padding="5" background="bg-subtle" id="sidemeny">
      <ul className="list-disc list-inside">
        {tiltakstype.kanKombineresMed.sort().map((tiltakstypen) => (
          <li key={tiltakstypen}>{tiltakstypen}</li>
        ))}
      </ul>
    </Box>
  );
}
