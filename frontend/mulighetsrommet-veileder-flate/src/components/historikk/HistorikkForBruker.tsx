import { ErrorMessage, Alert, Loader } from '@navikt/ds-react';
import classNames from 'classnames';
import { HistorikkForBruker as IHistorikkForBruker } from 'mulighetsrommet-api-client';
import { useHentHistorikk } from '../../core/api/queries/useHentHistorikk';
import { formaterDato } from '../../utils/Utils';
import styles from './HistorikkForBruker.module.scss';
import { StatusBadge } from './Statusbadge';

export function HistorikkForBruker() {
  const { data, isLoading, isError } = useHentHistorikk();
  if (isLoading) return <Loader />;

  if (isError) return <Alert variant="error">Kunne ikke hente brukerens tiltakshistorikk</Alert>;

  if (data?.length! === 0) {
    return <Alert variant="info">Fant ikke historikk for bruker</Alert>;
  }

  const sorterPaaFraDato = (a: IHistorikkForBruker, b: IHistorikkForBruker) => {
    if (!a.fraDato || !b.fraDato) return -1; // Flytt deltakelser uten fraDato bakerst

    return new Date(a.fraDato ?? '').getTime() - new Date(b.fraDato ?? '').getTime();
  };

  const venter = data?.filter(deltak => ['VENTER'].includes(deltak.status ?? '')).sort(sorterPaaFraDato) ?? [];
  const deltar = data?.filter(deltak => ['DELTAR'].includes(deltak.status ?? '')).sort(sorterPaaFraDato) ?? [];
  const avsluttet = data?.filter(deltak => ['AVSLUTTET'].includes(deltak.status ?? '')).sort(sorterPaaFraDato) ?? [];
  const ikkeAktuell =
    data?.filter(deltak => ['IKKE_AKTUELL'].includes(deltak.status ?? '')).sort(sorterPaaFraDato) ?? [];

  const tiltak = [...venter, ...deltar, ...avsluttet, ...ikkeAktuell];

  return (
    <div className={styles.historikk_for_bruker}>
      <ul className={styles.historikk_for_bruker_liste}>
        {tiltak?.map(historikk => {
          return (
            <li key={historikk.id} className={styles.historikk_for_bruker_element}>
              <div>
                <h1 className={classNames(styles.historikk_for_bruker_heading, 'navds-heading navds-heading--small')}>
                  {historikk.tiltaksnavn}
                </h1>
                <div className={styles.historikk_for_bruker_metadata}>
                  <p className={styles.historikk_text_content}>{historikk.tiltakstype}</p>
                  {historikk.arrangor ? (
                    <p className={styles.historikk_text_content}>{historikk.arrangor}</p>
                  ) : (
                    <ErrorMessage size="small">Tiltaksarrangør</ErrorMessage>
                  )}
                </div>
                <p className={classNames(styles.historikk_text_content, styles.historikk_datoer)}>
                  <span> {formaterDato(historikk.fraDato ?? '')}</span> -{' '}
                  <span>{formaterDato(historikk.tilDato ?? '')}</span>
                </p>
              </div>
              <aside style={{ justifySelf: 'end' }}>
                <StatusBadge status={historikk.status} />
              </aside>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
