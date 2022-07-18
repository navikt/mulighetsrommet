import React from 'react';
import { BodyShort, Heading, HelpText } from '@navikt/ds-react';
import './Statistikk.less';
import { StatistikkKomponent } from '../../api/models';

interface StatistikkProps {
  statistikkKomponent: StatistikkKomponent[];
}

const Statistikk = ({ statistikkKomponent }: StatistikkProps) => {
  return (
    <div className="statistikk">
      {statistikkKomponent.map((statistikk: any, index: number) => {
        return (
          <div className="statistikk__wrapper" key={index}>
            <div className="statistikk__heading">
              <Heading size="xsmall" level="2">
                {statistikk.statistikkOverskrift}
              </Heading>
              {statistikk.statistikkHjelpetekst && <HelpText>{statistikk.statistikkHjelpetekst}</HelpText>}
            </div>
            <BodyShort className="statistikk__tekst">{statistikk.statistikkInnhold}</BodyShort>
          </div>
        );
      })}
    </div>
  );
};

export default Statistikk;
