import React from 'react';
import { useLocation } from 'react-router-dom';
import Link from '../../../components/link/Link';
import { Tiltaksgjennomforing } from '../../../core/domain/Tiltaksgjennomforing';
import './TiltaksgjennomforingTabell.less';

interface TiltaksgjennomforingTabellProps {
  tiltaksgjennomforinger?: Tiltaksgjennomforing[];
}

function TiltaksgjennomforingsTabell(props: TiltaksgjennomforingTabellProps) {
  const location = useLocation();
  const { tiltaksgjennomforinger } = props;

  return (
    <table className="tabell tabell--stripet tabell__tiltaksgjennomforing" data-testid="tabell_tiltaksgjennomforinger">
      <thead>
        <tr>
          <th>Tittel</th>
          <th>Tiltaksnummer</th>
          <th>Til dato</th>
          <th>Fra dato</th>
        </tr>
      </thead>
      <tbody>
        {tiltaksgjennomforinger?.map(tiltaksgjennomforing => {
          return (
            <tr key={tiltaksgjennomforing.id} className="tabell__row">
              <td>
                <Link to={`${location.pathname}/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`}>
                  {tiltaksgjennomforing.tittel}
                </Link>
              </td>
              <td>{tiltaksgjennomforing.tiltaksnummer}</td>
              <td>{tiltaksgjennomforing.tilDato}</td>
              <td>{tiltaksgjennomforing.fraDato}</td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

export default TiltaksgjennomforingsTabell;
