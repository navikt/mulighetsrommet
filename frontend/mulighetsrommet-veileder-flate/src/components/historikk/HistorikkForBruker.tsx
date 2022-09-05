import { Alert, Loader } from '@navikt/ds-react';
import classNames from 'classnames';
import { Deltakerstatus, useHentHistorikk } from '../../core/api/queries/useHentHistorikk';
import { formaterDato } from '../../utils/Utils';
import './HistorikkForBruker.less';

function StatusBadge({ status }: { status: Deltakerstatus }) {
  return (
    <div
      className={classNames('historikk-for-bruker-statusbadge', `historikk-for-bruker-statusbadgde-farge-${status}`)}
    >
      {statustekst(status)}
    </div>
  );
}

function statustekst(status: Deltakerstatus): string {
  switch (status) {
    case 'AVSLUTTET':
      return 'Fullført';
    case 'DELTAR':
      return 'Deltar';
    case 'IKKE_AKTUELL':
      return 'Ikke aktuell';
    case 'VENTER':
      return 'Venter på oppstart';
  }
}

export function HistorikkForBruker() {
  const { data, isLoading, isError } = useHentHistorikk();
  if (isLoading) return <Loader />;

  if (isError) return <Alert variant="error">Klarte ikke hente historikk for bruker</Alert>;

  return (
    <div className="historikk-for-bruker">
      <ul className="historikk-for-bruker-liste">
        {data?.map(historikk => {
          return (
            <li key={historikk.id} className="historikk-for-bruker-element">
              <div>
                <h3 className="historikk-for-bruker-heading">{historikk.tiltaksnavn}</h3>
                <div className="historikk-for-bruker-metadata">
                  <span>{historikk.tiltakstype}</span>
                  <span>{historikk.arrangor}</span>
                </div>
                <span>
                  {formaterDato(historikk.fra_dato)} - {formaterDato(historikk.til_dato)}
                </span>
              </div>
              <div>
                <StatusBadge status={historikk.status} />
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
