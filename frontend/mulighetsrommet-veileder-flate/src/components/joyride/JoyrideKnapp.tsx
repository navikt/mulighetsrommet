import { Button } from "@navikt/ds-react";
import { HikingTrailSignIcon } from "@navikt/aksel-icons";

interface Props {
  handleClick: () => void;
  className?: string;
}

export const JoyrideKnapp = ({ handleClick, className }: Props) => {
  return (
    <Button
      size="small"
      variant="tertiary"
      onClick={handleClick}
      id="joyride_knapp"
      className={className}
    >
      <HikingTrailSignIcon title="Veiviser" />
      Veiviser
    </Button>
  );
};
