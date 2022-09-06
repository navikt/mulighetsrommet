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
                <h1 className="historikk-for-bruker-heading navds-heading navds-heading--small">
                  {historikk.tiltaksnavn}
                </h1>
                <div className="historikk-for-bruker-metadata">
                  <p className="historikk-text-content">{historikk.tiltakstype}</p>
                  <p className="historikk-text-content">{historikk.arrangor}</p>
                </div>
                <p className="historikk-text-content">
                  {formaterDato(historikk.fra_dato)} - {formaterDato(historikk.til_dato)}
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
