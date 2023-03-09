import { intervalToDuration, startOfDay } from 'date-fns';
import { BodyShort } from '@navikt/ds-react';

const CountDown = () => {
  const dagerTilPilot = numberOfDaysUntilPilot();

  if (dagerTilPilot <= 0) {
    return <BodyShort size="small">Tjenesten vil bli tilgjengelig i dag.</BodyShort>;
  }
  return (
    <>
      <BodyShort size="small">Tjenesten vil bli tilgjengelig om </BodyShort>
      <BodyShort size="small" style={{ fontWeight: 'bold' }}>
        {dagerTilPilot}
      </BodyShort>
      <BodyShort size="small">{dagerTilPilot > 1 ? 'dager.' : 'dag.'}</BodyShort>
    </>
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
