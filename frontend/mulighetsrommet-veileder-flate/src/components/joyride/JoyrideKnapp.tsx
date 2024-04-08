import { Button } from "@navikt/ds-react";
import { HikingTrailSignIcon } from "@navikt/aksel-icons";
import styles from "./Joyride.module.scss";

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
      className={styles.joyride_knapp}
    >
      <HikingTrailSignIcon title="Veiviser" />
      Veiviser
    </Button>
  );
}
