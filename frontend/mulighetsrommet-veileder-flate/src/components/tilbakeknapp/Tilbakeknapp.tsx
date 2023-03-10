import { Back } from '@navikt/ds-icons';
import Lenke from '../lenke/Lenke';
import styles from './Tilbakeknapp.module.scss';
import { BodyShort } from '@navikt/ds-react';

interface TilbakeknappProps {
  tilbakelenke: string;
  tekst?: string;
}

const Tilbakeknapp = ({ tilbakelenke, tekst = 'Tilbake' }: TilbakeknappProps) => {
  return (
    <Lenke className={styles.tilbakeknapp} to={tilbakelenke} data-testid="tilbakeknapp">
      <Back aria-label="Tilbakeknapp" />
      <BodyShort size="small">{tekst}</BodyShort>
    </Lenke>
  );
};
export default Tilbakeknapp;
