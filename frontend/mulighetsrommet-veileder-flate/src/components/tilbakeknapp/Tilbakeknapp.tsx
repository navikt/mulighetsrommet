import { Back } from '@navikt/ds-icons';
import Lenke from '../lenke/Lenke';
import styles from './Tilbakeknapp.module.scss';

interface TilbakeknappProps {
  tilbakelenke: string;
}

const Tilbakeknapp = ({ tilbakelenke }: TilbakeknappProps) => {
  return (
    <Lenke className={styles.tilbakeknapp} to={tilbakelenke} data-testid="tilbakeknapp">
      <Back aria-label="Tilbakeknapp" />
      <span>Tilbake</span>
    </Lenke>
  );
};
export default Tilbakeknapp;
