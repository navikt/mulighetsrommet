import { formaterPeriode, subDuration } from "@mr/frontend-common/utils/date";
import { PersonIcon, ParasolBeachIcon } from "@navikt/aksel-icons";
import { Timeline } from "@navikt/ds-react";
import { ArrFlateBeregningDeltakelse, Periode, UtbetalingStengtPeriode } from "api-client";

interface DeltakelseTimelineProps {
  utbetalingsperiode: Periode;
  stengt: UtbetalingStengtPeriode[];
  deltakelse: ArrFlateBeregningDeltakelse;
}

export function DeltakelseTimeline({
  utbetalingsperiode,
  stengt,
  deltakelse,
}: DeltakelseTimelineProps) {
  return (
    <Timeline
      startDate={new Date(utbetalingsperiode.start)}
      endDate={new Date(utbetalingsperiode.slutt)}
    >
      <Timeline.Row label="Deltakelse" icon={<PersonIcon aria-hidden />}>
        {perioder(deltakelse).map((p) => (
          <Timeline.Period
            key={p.key}
            start={p.start}
            end={p.end}
            status={"success"}
            icon={<PersonIcon aria-hidden />}
            statusLabel={p.label}
          >
            {p.label}
          </Timeline.Period>
        ))}
      </Timeline.Row>
      <Timeline.Row label="Stengt" icon={<ParasolBeachIcon aria-hidden />}>
        {stengt.map(({ periode, beskrivelse }) => {
          const start = new Date(periode.start);
          const end = subDuration(new Date(periode.slutt), { days: 1 })!;
          const label = `${formaterPeriode(periode)}: ${beskrivelse}`;
          return (
            <Timeline.Period
              key={periode.start + periode.slutt}
              start={start}
              end={end}
              status={"info"}
              icon={<ParasolBeachIcon aria-hidden />}
              statusLabel={label}
            >
              {label}
            </Timeline.Period>
          );
        })}
      </Timeline.Row>
    </Timeline>
  );
}

interface TimelinePeriodeData {
  start: Date;
  end: Date;
  key: string;
  label: string;
}

function perioder(deltakelse: ArrFlateBeregningDeltakelse): TimelinePeriodeData[] {
  switch (deltakelse.type) {
    case "PRIS_PER_MANEDSVERK_MED_DELTAKELSESMENGDER": {
      return deltakelse.perioderMedDeltakelsesmengde.map((p) => {
        const start = new Date(p.periode.start);
        const end = subDuration(new Date(p.periode.slutt), { days: 1 })!;
        const label = `${formaterPeriode(p.periode)}: Deltakelse på ${p.deltakelsesprosent}%`;

        return {
          key: p.periode.start + p.periode.slutt,
          start,
          end,
          label,
        };
      });
    }
    case "FRI":
    case "PRIS_PER_MANEDSVERK":
    case "PRIS_PER_UKESVERK": {
      const start = new Date(deltakelse.periode.start);
      const end = subDuration(new Date(deltakelse.periode.slutt), { days: 1 })!;
      const label = formaterPeriode(deltakelse.periode);

      return [
        {
          key: deltakelse.periode.start + deltakelse.periode.slutt,
          start,
          end,
          label,
        },
      ];
    }
  }
}
