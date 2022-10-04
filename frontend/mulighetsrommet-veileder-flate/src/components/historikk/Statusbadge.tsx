import classNames from 'classnames';
import { HistorikkForBruker } from 'mulighetsrommet-api-client';
import styles from './Statusbadge.module.scss';

export function StatusBadge({ status }: { status?: HistorikkForBruker.status }) {
  return (
    <div className={classNames(styles.historikkForBrukerStatusbadge, styles[status as unknown as any])}>
      {statustekst(status)}
    </div>
  );
}

function statustekst(status?: HistorikkForBruker.status): string {
  switch (status) {
    case 'AVSLUTTET':
      return 'Avsluttet';
    case 'DELTAR':
      return 'Deltar';
    case 'IKKE_AKTUELL':
      return 'Ikke aktuell';
    case 'VENTER':
      return 'Venter';
    default:
      return '';
  }
}
