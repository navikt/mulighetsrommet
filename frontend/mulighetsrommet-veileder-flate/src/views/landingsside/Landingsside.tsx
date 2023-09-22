import { PlusIcon } from '@navikt/aksel-icons';
import { HistorikkForBrukerModalInnhold } from '../../components/historikk/HistorikkForBrukerModalInnhold';
import { routes } from '../../routes';
import styles from './Landingsside.module.scss';

export function Landingsside() {
  return (
    <main className="mulighetsrommet-veileder-flate">
      <div className={styles.container}>
        <div>
          <a className={styles.cta_link} href={`/${routes.oversikt()}`}>
            <PlusIcon color="white" fontSize={30} aria-hidden /> Finn nytt arbeidsmarkedstiltak
          </a>
        </div>
        <div>
          <h3>Tiltakshistorikk</h3>
          <HistorikkForBrukerModalInnhold />
        </div>
      </div>
    </main>
  );
}
