import { Heading, HelpText } from '@navikt/ds-react';
import { NokkelinfoKomponenter } from '../../core/api/models';
import styles from './Nokkelinfo.module.scss';

export interface NokkelinfoProps {
  nokkelinfoKomponenter: NokkelinfoKomponenter[];
}

const Nokkelinfo = ({ nokkelinfoKomponenter, ...rest }: NokkelinfoProps) => {
  return (
    <div className={styles.container} {...rest}>
      {nokkelinfoKomponenter.map((nokkelinfo: NokkelinfoKomponenter, index: number) => {
        return (
          <div className={styles.nokkelinfo} key={index}>
            <div className={styles.content}>
              {typeof nokkelinfo.innhold === 'string' ? (
                <p className={styles.tekst}>{nokkelinfo.innhold}</p>
              ) : (
                <div className={styles.tekst}>{nokkelinfo.innhold}</div>
              )}

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
