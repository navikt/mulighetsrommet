import { Timeline } from "@navikt/ds-react";
import { TimelineDto } from "@api-client";

interface Props {
  data: TimelineDto;
}

export function DataDrivenTimeline({ data }: Props) {
  return (
    <Timeline startDate={new Date(data.startDate)} endDate={new Date(data.endDate)}>
      {data.rows.map((row) => (
        <Timeline.Row label="Beregning">
          {row.periods.map((period) => (
            <Timeline.Period
              key={period.key}
              start={new Date(period.start)}
              end={new Date(period.end)}
              status={"info"}
              icon={period.content}
            >
              {period.content}
            </Timeline.Period>
          ))}
        </Timeline.Row>
      ))}
    </Timeline>
  );
}
