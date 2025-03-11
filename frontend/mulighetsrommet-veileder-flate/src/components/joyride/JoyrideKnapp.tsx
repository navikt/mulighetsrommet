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
      id="joyride_knapp"
      className="[&_.navds-label]:flex [&_.navds-label]:flex-row [&_.navds-label]:items-center [&_.navds-label]:gap-[2px] [&_svg]:m-0 [&_svg]:w-6 [&_svg]:h-auto"
    >
      <HikingTrailSignIcon title="Veiviser" />
      Veiviser
    </Button>
  );
}
