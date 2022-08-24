import React from 'react';
import { BodyShort, Heading, HelpText } from '@navikt/ds-react';
import './Nokkelinfo.less';
import { NokkelinfoKomponenter } from '../../core/api/models';

interface NokkelinfoProps {
  nokkelinfoKomponenter: NokkelinfoKomponenter[];
}

const Nokkelinfo = ({ nokkelinfoKomponenter }: NokkelinfoProps) => {
  return (
    <div className="nokkelinfo">
      {nokkelinfoKomponenter.map((nokkelinfo: NokkelinfoKomponenter, index: number) => {
        return (
          <div className="nokkelinfo__container" key={index}>
            <div className="nokkelinfo__heading">
              <Heading size="xsmall" level="2">
                {nokkelinfo.tittel}
              </Heading>
              {nokkelinfo.hjelpetekst && (
                <HelpText title="Se hvordan prosenten er regnet ut" placement="right" style={{ maxWidth: '400px' }}>
                  {nokkelinfo.hjelpetekst}
                </HelpText>
              )}
            </div>
            <BodyShort className="nokkelinfo__tekst">{nokkelinfo.innhold}</BodyShort>
          </div>
        );
      })}
    </div>
  );
};

export default Nokkelinfo;
