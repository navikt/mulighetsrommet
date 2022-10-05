import { PortableText } from '@portabletext/react';
import { Forskningsrapport as ForskningsrapportType } from '../../../core/api/models';
import styles from '../Detaljerfane.module.scss';
import forskningStyles from './Forskningsrapport.module.scss';

interface Props {
  forskningsrapporter: ForskningsrapportType[];
}

export function Forskningsrapport({ forskningsrapporter }: Props) {
  return (
    <section>
      {forskningsrapporter.map(rapport => {
        return (
          <div key={rapport._id}>
            <h2 className={styles.tiltaksdetaljer_innsiktheader}>{rapport.tittel}</h2>
            <PortableText value={rapport.beskrivelse} />
            <ul className={forskningStyles.forskningsrapport__lenkeliste}>
              {rapport.lenker?.map(({ lenke, lenkenavn }, index) => {
                return (
                  <li key={index}>
                    <a href={lenke}>{lenkenavn}</a>
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
