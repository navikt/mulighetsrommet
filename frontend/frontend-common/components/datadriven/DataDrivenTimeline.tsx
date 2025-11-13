import { Timeline } from "@navikt/ds-react";
import { TimelineDto, TimelineDtoRowPeriodVariant } from "./types";

interface Props {
  data: TimelineDto;
}

export function DataDrivenTimeline({ data }: Props) {
  return (
    <Timeline startDate={new Date(data.startDate)} endDate={new Date(data.endDate)}>
      {data.rows.map((row) => (
        <Timeline.Row label={row.label}>
          {row.periods.map((period) => (
            <Timeline.Period
              key={period.key}
              start={new Date(period.start)}
              end={new Date(period.end)}
              status={getStatusVariant(period.status)}
              icon={period.content}
            >
              {period.hover}
            </Timeline.Period>
          ))}
        </Timeline.Row>
      ))}
    </Timeline>
  );
}

function getStatusVariant(
  variant: TimelineDtoRowPeriodVariant,
): "success" | "warning" | "danger" | "info" | "neutral" {
  switch (variant) {
    case TimelineDtoRowPeriodVariant.INFO:
      return "info";
    case TimelineDtoRowPeriodVariant.SUCCESS:
      return "success";
    case TimelineDtoRowPeriodVariant.WARNING:
      return "warning";
    case TimelineDtoRowPeriodVariant.DANGER:
      return "danger";
    case TimelineDtoRowPeriodVariant.NEUTRAL:
      return "neutral";
  }
}
