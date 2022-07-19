import React from 'react';
import { BodyShort, Heading, HelpText } from '@navikt/ds-react';
import './Statistikk.less';
import { StatistikkKomponenter } from '../../api/models';

interface StatistikkProps {
  statistikkKomponenter: StatistikkKomponenter[];
}

const Statistikk = ({ statistikkKomponenter }: StatistikkProps) => {
  return (
    <div className="statistikk">
      {statistikkKomponenter.map((statistikk: any, index: number) => {
        return (
          <div className="statistikk__wrapper" key={index}>
            <div className="statistikk__heading">
              <Heading size="xsmall" level="2">
                {statistikk.overskrift}
              </Heading>
              {statistikk.hjelpetekst && <HelpText>{statistikk.hjelpetekst}</HelpText>}
            </div>
            <BodyShort className="statistikk__tekst">{statistikk.innhold}</BodyShort>
          </div>
        );
      })}
    </div>
  );
};

export default Statistikk;
