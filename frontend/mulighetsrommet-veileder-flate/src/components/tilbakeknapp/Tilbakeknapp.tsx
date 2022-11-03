import { Back } from '@navikt/ds-icons';
import Lenke from '../lenke/Lenke';
import styles from './Tilbakeknapp.module.scss';

interface TilbakeknappProps {
  tilbakelenke: string;
  tekst?: string;
}

const Tilbakeknapp = ({ tilbakelenke, tekst = 'Tilbake' }: TilbakeknappProps) => {
  return (
    <Lenke className={styles.tilbakeknapp} to={tilbakelenke} data-testid="tilbakeknapp">
      <Back aria-label="Tilbakeknapp" />
      <span>{tekst}</span>
    </Lenke>
  );
};
export default Tilbakeknapp;
