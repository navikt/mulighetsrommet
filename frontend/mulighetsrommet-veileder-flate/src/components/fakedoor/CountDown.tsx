import { intervalToDuration, startOfDay } from 'date-fns';

const CountDown = () => {
  const dagerTilPilot = numberOfDaysUntilPilot();

  if (dagerTilPilot <= 0) {
    return (
      <div>
        <span>Tjenesten vil bli tilgjengelig i dag.</span>
      </div>
    );
  }
  return (
    <div>
      <span>Tjenesten vil bli tilgjengelig om </span>
      <span style={{ fontWeight: 'bold' }}> {dagerTilPilot} </span>
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
