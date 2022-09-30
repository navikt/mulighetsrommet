import { BodyShort, Heading, HelpText } from '@navikt/ds-react';
import { NokkelinfoKomponenter } from '../../core/api/models';
import styles from './Nokkelinfo.module.scss';

interface NokkelinfoProps {
  nokkelinfoKomponenter: NokkelinfoKomponenter[];
}

const Nokkelinfo = ({ nokkelinfoKomponenter }: NokkelinfoProps) => {
  return (
    <div className={styles.container}>
      {nokkelinfoKomponenter.map((nokkelinfo: NokkelinfoKomponenter, index: number) => {
        return (
          <div className={styles.nokkelinfo} key={index}>
            <div className={styles.heading}>
              <Heading size="xsmall" level="2">
                {nokkelinfo.tittel}
              </Heading>
              {nokkelinfo.hjelpetekst && (
                <HelpText title="Se hvordan prosenten er regnet ut" placement="right" style={{ maxWidth: '400px' }}>
                  {nokkelinfo.hjelpetekst}
                </HelpText>
              )}
            </div>
            <BodyShort className={styles.tekst}>{nokkelinfo.innhold}</BodyShort>
          </div>
        );
      })}
    </div>
  );
};

export default Nokkelinfo;
