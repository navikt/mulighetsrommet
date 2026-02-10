import { Button } from "@navikt/ds-react";
import { HikingTrailSignIcon } from "@navikt/aksel-icons";

interface Props {
  handleClick: () => void;
}

export function JoyrideKnapp({ handleClick }: Props) {
  return (
    <Button
      size="small"
      variant="tertiary"
      onClick={handleClick}
      icon={<HikingTrailSignIcon title="Veiviser" />}
      id="joyride_knapp"
      data-color="accent"
    >
      Veiviser
    </Button>
  );
}
