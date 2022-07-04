import React from 'react';
import AutoSizer from 'react-virtualized-auto-sizer';
import BarChart from './BarChart';
import '../TiltaksdetaljerFane.less';

export type InnsiktsFaneProps = {
  tiltakstype: string;
};

const tiltakstyperMedStatstikk = ['Oppfølging', 'Digital Oppfølging', 'Avklaring', 'ARR', 'AFT', 'Jobbklubb'];

const InnsiktsFane = ({ tiltakstype }: InnsiktsFaneProps) => {
  return (
    <div className={'tiltaksdetaljer__maksbredde'}>
      {tiltakstyperMedStatstikk.includes(tiltakstype) && (
        <>
          <AutoSizer disableHeight>
            {({ width }) => <BarChart tiltakstype={tiltakstype} width={width} height={300} />}
          </AutoSizer>
        </>
      )}
    </div>
  );
};

export default InnsiktsFane;
