import React from 'react';
import moment from 'moment';
import './CountDown.less';

const CountDown = () => {
  const dagerTilPilot = numberOfDaysUntilPilot()

  return (
    <div className="fakedoor-countdown">
        <span>Tjenesten vil bli tilgjengelig om </span>
      <span className={"fakedoor-countdown-days"}> {dagerTilPilot} </span>
      {dagerTilPilot != 1 && <span>dager.</span>}
      {dagerTilPilot == 1 && <span>dag.</span>}
    </div>
  );
};

function numberOfDaysUntilPilot() {
  const pilotStart = moment('2022-08-29', 'YYYY-MM-DD');
  const currentDate = moment().startOf('day');
  return moment.duration(pilotStart.diff(currentDate)).asDays();
}

export default CountDown;
