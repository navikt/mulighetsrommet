import React from 'react';
import './FakeDoor.less';
import CountDown from './CountDown';
import { Link } from '@navikt/ds-react';
import imgUrl from '../../../public/illustrasjonfakedoor.png';

const FakeDoor = () => {
  return (
    <div className="fakedoor-container">
      <div className="fakedoor-text-container">
        <h1 className="fakedoor-header">Pilot - Informasjonstjeneste for arbeidsmarkedtiltak</h1>
        <div className="fakedoor-text">
          <div>
            Her vil du som veileder kunne finne kvalitetssikret informasjon om de tiltakene som er aktuelle for din
            bruker, slik at du kan gjøre gode vurderinger, gi effektiv veiledning og få brukeren raskere ut i rett
            arbeidsmarkedstiltak.
          </div>
          <br />
          <CountDown />
          <br />
          <Link target="_blank" className="fakedoor-link" href="https://navno.sharepoint.com/:b:/s/team-valp/EZrCwwYUh-ZGr7CbYn1Lt8UBW3pNcabIfUeuZeWeuEcmaQ?e=1RH9RW">Les mer om piloteringen</Link>
        </div>
      </div>
      <div className="fakedoor-image">
        <img
          src={imgUrl}
          id="fakedoor-bilde"
          alt="Illustrasjon av løsning"
        />
      </div>
    </div>
  );
};

export default FakeDoor;
