import { Heading, HelpText } from '@navikt/ds-react';
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
            <div className={styles.content}>
              <p className={styles.tekst}>{nokkelinfo.innhold}</p>
              {nokkelinfo.hjelpetekst && (
                <HelpText title="Se hvordan prosenten er regnet ut" placement="right" style={{ maxWidth: '400px' }}>
                  {nokkelinfo.hjelpetekst}
                </HelpText>
              )}
            </div>
            <Heading className={styles.heading} size="xsmall" level="2">
              {nokkelinfo.tittel}
            </Heading>
          </div>
        );
      })}
    </div>
  );
};

export default Nokkelinfo;
