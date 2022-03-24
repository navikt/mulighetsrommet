import React from 'react';
import { BodyShort, Heading, HelpText } from '@navikt/ds-react';
import './Statistikk.less';

interface StatistikkBolkProps {
  tittel: string;
  hjelpetekst: string;
  statistikktekst: any;
}

const StatistikkBolk = ({ tittel, hjelpetekst, statistikktekst }: StatistikkBolkProps) => {
  return (
    <div className="statistikk__bolk">
      <div className="statistikk__bolk__heading">
        <Heading size="xsmall" level="3">
          {tittel}
        </Heading>
        <HelpText title="Hva er dette?">{hjelpetekst}</HelpText>
      </div>
      <BodyShort>{statistikktekst}</BodyShort>
    </div>
  );
};

export default StatistikkBolk;
