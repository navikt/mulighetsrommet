import React from 'react';
import { BodyShort, Heading, HelpText } from '@navikt/ds-react';
import './Statistikk.less';

interface StatistikkProps {
  tittel: string;
  hjelpetekst: string;
  statistikktekst: any;
}

const Statistikk = ({ tittel, hjelpetekst, statistikktekst }: StatistikkProps) => {
  return (
    <div className="statistikk">
      <div className="statistikk__heading">
        <Heading size="xsmall" level="2">
          {tittel}
        </Heading>
        <HelpText title="Hva er dette?">{hjelpetekst}</HelpText>
      </div>
      <BodyShort className="statistikk__tekst">{statistikktekst}</BodyShort>
    </div>
  );
};

export default Statistikk;
