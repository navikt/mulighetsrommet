import React from 'react';
import { Tiltaksvariant } from '../../../core/domain/Tiltaksvariant';
import Link from '../../link/Link';
import './TiltaksvariantRad.less';

interface TiltaksvariantRadProps {
  tiltaksvariant: Tiltaksvariant;
}

const TiltaksvariantRad = ({ tiltaksvariant }: TiltaksvariantRadProps) => {
  const { id, tittel, ingress } = tiltaksvariant;
  return (
    <tr key={id} className="tabell__row">
      <td>
        <Link to={`/tiltaksvarianter/${id}`} isInline>
          {tittel}
        </Link>
      </td>
      <td className="tabell__row__ingress">{ingress}</td>
    </tr>
  );
};

export default TiltaksvariantRad;
