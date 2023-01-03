import { PortableText } from '@portabletext/react';
import { Forskningsrapport as ForskningsrapportType } from '../../../core/api/models';
import styles from '../Detaljerfane.module.scss';
import forskningStyles from './Forskningsrapport.module.scss';
import { Heading } from '@navikt/ds-react';
import Lenke from '../../lenke/Lenke';

interface Props {
  forskningsrapporter: ForskningsrapportType[];
}

export function Forskningsrapport({ forskningsrapporter }: Props) {
  return (
    <section>
      {forskningsrapporter.map(rapport => {
        return (
          <div key={rapport._id}>
            <Heading size="small" className={styles.tiltaksdetaljer_innsiktheader}>
              {rapport.tittel}
            </Heading>
            <PortableText value={rapport.beskrivelse} />
            <ul className={forskningStyles.forskningsrapport_lenkeliste}>
              {rapport.lenker?.map(({ lenke, lenkenavn }, index) => {
                return (
                  <li key={index}>
                    <Lenke to={lenke}>{lenkenavn}</Lenke>
                  </li>
                );
              })}
            </ul>
          </div>
        );
      })}
    </section>
  );
}
