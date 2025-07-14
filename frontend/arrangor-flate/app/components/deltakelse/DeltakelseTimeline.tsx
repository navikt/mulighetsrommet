import { parseDate } from "@mr/frontend-common/utils/date";
import { PersonIcon, ParasolBeachIcon } from "@navikt/aksel-icons";
import { Timeline } from "@navikt/ds-react";
import { Periode, ArrFlateUtbetalingDeltakelse, UtbetalingStengtPeriode } from "api-client";
import { subDays } from "date-fns";
import { formaterPeriode } from "~/utils/date";

interface DeltakelseTimelineProps {
  utbetalingsperiode: Periode;
  stengt: UtbetalingStengtPeriode[];
  deltakelse: ArrFlateUtbetalingDeltakelse;
}

export function DeltakelseTimeline({
  utbetalingsperiode,
  stengt,
  deltakelse,
}: DeltakelseTimelineProps) {
  return (
    <Timeline
      startDate={parseDate(utbetalingsperiode.start)}
      endDate={parseDate(utbetalingsperiode.slutt)}
    >
      <Timeline.Row label="Deltakelse" icon={<PersonIcon aria-hidden />}>
        {deltakelse.perioderMedDeltakelsesmengde.map(({ periode, deltakelsesprosent }) => {
          const start = parseDate(periode.start);
          const end = subDays(periode.slutt, 1);
          const label = `${formaterPeriode(periode)}: Deltakelse på ${deltakelsesprosent}%`;
          return (
            <Timeline.Period
              key={periode.start + periode.slutt}
              start={start!}
              end={end}
              status={"success"}
              icon={<PersonIcon aria-hidden />}
              statusLabel={label}
            >
              {label}
            </Timeline.Period>
          );
        })}
      </Timeline.Row>
      <Timeline.Row label="Stengt" icon={<ParasolBeachIcon aria-hidden />}>
        {stengt.map(({ periode, beskrivelse }) => {
          const start = parseDate(periode.start);
          const end = subDays(parseDate(periode.slutt) ?? "", 1);
          const label = `${formaterPeriode(periode)}: ${beskrivelse}`;
          return (
            <Timeline.Period
              key={periode.start + periode.slutt}
              start={start!}
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
