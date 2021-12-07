import React from 'react';
import TiltaksvariantRad from './TiltaksvariantRad';
import { Tiltaksvariant } from '../../../core/domain/Tiltaksvariant';

export interface TiltaksvariantlisteProps {
  tiltaksvariantliste?: Array<Tiltaksvariant>;
}

const Tiltaksvariantliste = ({ tiltaksvariantliste }: TiltaksvariantlisteProps) => {
  return (
    <table className="tabell tabell--stripet" data-testid="tabell_oversikt-tiltaksvarianter">
      <thead>
        <tr>
          <th>Tittel</th>
          <th>Ingress</th>
        </tr>
      </thead>
      <tbody>
        {tiltaksvariantliste &&
          tiltaksvariantliste?.map((tiltaksvariant: Tiltaksvariant) => (
            <TiltaksvariantRad key={tiltaksvariant.id} tiltaksvariant={tiltaksvariant} />
          ))}
      </tbody>
    </table>
  );
};

export default Tiltaksvariantliste;
