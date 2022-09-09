import { Alert, Loader } from '@navikt/ds-react';
import classNames from 'classnames';
import { useHentHistorikk } from '../../core/api/queries/useHentHistorikk';
import { HistorikkForBruker as IHistorikkForBruker } from 'mulighetsrommet-api-client';
import { formaterDato } from '../../utils/Utils';
import './HistorikkForBruker.less';

function StatusBadge({ status }: { status?: IHistorikkForBruker.status }) {
  return (
    <div
      className={classNames('historikk-for-bruker-statusbadge', `historikk-for-bruker-statusbadgde-farge-${status}`)}
    >
      {statustekst(status)}
    </div>
  );
}

function statustekst(status?: IHistorikkForBruker.status): string {
  switch (status) {
    case 'AVSLUTTET':
      return 'Fullført';
    case 'DELTAR':
      return 'Deltar';
    case 'IKKE_AKTUELL':
      return 'Ikke aktuell';
    case 'VENTER':
      return 'Venter på oppstart';
    default:
      return '';
  }
}

export function HistorikkForBruker() {
  const { data, isLoading, isError } = useHentHistorikk();
  if (isLoading) return <Loader />;

  if (isError) return <Alert variant="error">Klarte ikke hente historikk for bruker</Alert>;

  if (data?.length! === 0) {
    return <Alert variant="info">Fant ikke historikk for bruker</Alert>;
  }

  return (
    <div className="historikk-for-bruker">
      <ul className="historikk-for-bruker-liste">
        {data?.map(historikk => {
          return (
            <li key={historikk.id} className="historikk-for-bruker-element">
              <div>
                <h1 className="historikk-for-bruker-heading navds-heading navds-heading--small">
                  {historikk.tiltaksnavn}
                </h1>
                <div className="historikk-for-bruker-metadata">
                  <p className="historikk-text-content">{historikk.tiltakstype}</p>
                  <p className="historikk-text-content">{historikk.arrangor}</p>
                </div>
                <p className="historikk-text-content">
                  {[historikk.fraDato, historikk.tilDato]
                    .filter(Boolean)
                    .map(dato => formaterDato(dato!))
                    .join(' - ')}
                </p>
              </div>
              <aside>
                <StatusBadge status={historikk.status} />
              </aside>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
