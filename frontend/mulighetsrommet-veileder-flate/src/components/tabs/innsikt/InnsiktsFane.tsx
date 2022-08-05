import { Alert } from '@navikt/ds-react';
import useResizeObserver from 'use-resize-observer';
import useTiltaksgjennomforingByTiltaksnummer from '../../../api/queries/useTiltaksgjennomforingByTiltaksnummer';
import { useTiltakstyperMedTiltakstypenavn } from '../../../api/queries/useTiltakstypeMedTiltakstypenavn';
import '../TiltaksdetaljerFane.less';
import BarChart from './BarChart';
import { Forskningsrapport } from './Forskningsrapport';

export type InnsiktsFaneProps = {
  tiltakstype: string;
};

const tiltakstyperMedStatistikk = ['Oppfølging', 'Digital Oppfølging', 'ARR', 'AFT', 'Jobbklubb'];

const InnsiktsFane = ({ tiltakstype }: InnsiktsFaneProps) => {
  const { ref, width = 500 } = useResizeObserver<HTMLDivElement>({});
  const { data } = useTiltakstyperMedTiltakstypenavn(tiltakstype);
  const forskningsrapporter = data?.forskningsrapport;
  return (
    <div className={'tiltaksdetaljer__maksbredde'}>
      {tiltakstyperMedStatistikk.includes(tiltakstype) ? (
        <div ref={ref}>
          <BarChart tiltakstype={tiltakstype} width={width} height={300} />
        </div>
      ) : (
        <Alert variant="info">
          Det finnes ikke statistikkgrunnlag for tiltakstypen <b>{tiltakstype}</b>
        </Alert>
      )}
      {forskningsrapporter ? <Forskningsrapport forskningsrapporter={forskningsrapporter} /> : null}
    </div>
  );
};

export default InnsiktsFane;
