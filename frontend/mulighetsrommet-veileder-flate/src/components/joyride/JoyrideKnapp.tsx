import { Button } from '@navikt/ds-react';
import { HikingTrailSignIcon } from '@navikt/aksel-icons';
import style from './Joyride.module.scss';

interface Props {
  handleClick: () => void;
}

export const JoyrideKnapp = ({ handleClick }: Props) => {
  return (
    <Button variant="tertiary" onClick={handleClick} id="joyride_knapp" className={style.joyride_knapp}>
      <HikingTrailSignIcon title="Veiviser" fontSize="1.7rem" />
      Veiviser
    </Button>
  );
};
