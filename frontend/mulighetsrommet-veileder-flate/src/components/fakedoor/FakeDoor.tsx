import React from 'react';
import './FakeDoor.less';
import CountDown from "./CountDown";

const FakeDoor = () => {
  return (
    <div className="fakedoor-container">
      <h1 className="fakedoor-header">Pilot - Informasjonstjeneste for arbeidsmarkedtiltak</h1>
      <div className="fakedoor-text">
        <div>
          Her vil du som veileder kunne finne kvalitetssikret informasjon om de tiltakene som er aktuelle for din
          bruker, slik at du kan gjøre gode vurderinger, gi effektiv veiledning og få brukeren raskere ut i rett
          arbeidsmarkedstiltak.{' '}
        </div>
        <CountDown />
        <div>Les mer om piloteringen</div>
      </div>
    </div>
  );
};

function numberOfDaysUntilPilot() {
  return 1;
}

export default FakeDoor;
