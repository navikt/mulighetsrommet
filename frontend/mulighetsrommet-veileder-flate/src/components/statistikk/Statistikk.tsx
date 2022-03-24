import React from 'react';
import './Statistikk.less';
import StatistikkBolk from './StatistikkBolk';
import { Tag } from '@navikt/ds-react';

interface StatistikkProps {
  innsatsgruppe: number | null | undefined;
}
const Statistikk = ({ innsatsgruppe }: StatistikkProps) => {
  const velgInnsatsgruppe = () => {
    if (innsatsgruppe === 1) {
      return <Tag variant="info">Si</Tag>;
    } else if (innsatsgruppe === 2) {
      return <Tag variant="info">Sbi</Tag>;
    } else if (innsatsgruppe === 3) {
      return <Tag variant="info">Sti</Tag>;
    } else if (innsatsgruppe === 4) {
      return <Tag variant="info">Vti</Tag>;
    }
  };

  return (
    <div className="statistikk">
      <StatistikkBolk
        tittel="Overgang til arbeid"
        hjelpetekst="Her skal det stå litt om hva denne statistikken viser oss"
        statistikktekst="0%"
      />
      <StatistikkBolk
        tittel="Oppstart"
        hjelpetekst="Her skal det stå litt om hva denne statistikken viser oss"
        statistikktekst="Løpende"
      />
      <StatistikkBolk
        tittel="Varighet"
        hjelpetekst="Her skal det stå litt om hva denne statistikken viser oss"
        statistikktekst="8 uker"
      />
      <StatistikkBolk
        tittel="Innsatsgruppe"
        hjelpetekst="Her skal det stå litt om hva denne statistikken viser oss"
        statistikktekst={velgInnsatsgruppe()}
      />
    </div>
  );
};

export default Statistikk;
