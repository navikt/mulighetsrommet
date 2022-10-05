import { Alert, Loader } from '@navikt/ds-react';
import useResizeObserver from 'use-resize-observer';
import { useTiltakstyperMedTiltakstypenavn } from '../../../core/api/queries/useTiltakstypeMedTiltakstypenavn';
import styles from '../Detaljerfane.module.scss';
import BarChart from './BarChart';
import { Forskningsrapport } from './Forskningsrapport';

export type InnsiktsFaneProps = {
  tiltakstype: string;
};

const tiltakstyperMedStatistikk = ['Oppfølging', 'Digital Oppfølging', 'ARR', 'AFT', 'Jobbklubb'];

const InnsiktsFane = ({ tiltakstype }: InnsiktsFaneProps) => {
  const { ref, width = 500 } = useResizeObserver<HTMLDivElement>({});
  const { data, isLoading } = useTiltakstyperMedTiltakstypenavn(tiltakstype);
  const forskningsrapporter = data?.forskningsrapport;

  if (isLoading) {
    return <Loader />;
  }

  if (!forskningsrapporter && !tiltakstyperMedStatistikk.includes(tiltakstype)) {
    return (
      <Alert variant="info">
        Det finnes ikke statistikkgrunnlag for tiltakstypen <b>{tiltakstype}</b>
      </Alert>
    );
  }

  return (
    <div className={styles.tiltaksdetaljer__maksbredde}>
      {tiltakstyperMedStatistikk.includes(tiltakstype) ? (
        <div style={{ marginBottom: '2rem' }} ref={ref}>
          <BarChart tiltakstype={tiltakstype} width={width} height={300} />
        </div>
      ) : null}
      {forskningsrapporter ? <Forskningsrapport forskningsrapporter={forskningsrapporter} /> : null}
    </div>
  );
};

export default InnsiktsFane;
