import { intervalToDuration, startOfDay } from 'date-fns';
import './CountDown.less';

const CountDown = () => {
  const dagerTilPilot = numberOfDaysUntilPilot();

  if (dagerTilPilot <= 0) {
    return (
      <div className="fakedoor-countdown">
        <span>Tjenesten vil bli tilgjengelig i dag.</span>
      </div>
    );
  }
  return (
    <div className="fakedoor-countdown">
      <span>Tjenesten vil bli tilgjengelig om </span>
      <span className={'fakedoor-countdown-days'}> {dagerTilPilot} </span>
      <span>{dagerTilPilot > 1 ? 'dager.' : 'dag.'}</span>
    </div>
  );
};

function numberOfDaysUntilPilot() {
  const pilotStart = startOfDay(new Date('2022-08-29'));
  const currentDate = startOfDay(new Date());
  return (
    intervalToDuration({
      start: currentDate,
      end: pilotStart,
    }).days ?? 0
  );
}

export default CountDown;
