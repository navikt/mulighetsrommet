import { PortableText } from '@portabletext/react';
import { Forskningsrapport as ForskningsrapportType } from '../../../core/api/models';
import '../TiltaksdetaljerFane.less';

interface Props {
  forskningsrapporter: ForskningsrapportType[];
}

export function Forskningsrapport({ forskningsrapporter }: Props) {
  return (
    <section className="forskningsrapport__container">
      {forskningsrapporter.map(rapport => {
        return (
          <div key={rapport._id}>
            <h2 className="tiltaksdetaljer__innsiktheader">{rapport.tittel}</h2>
            <PortableText value={rapport.beskrivelse} />
            <ul className="forskningsrapport__lenkeliste">
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
