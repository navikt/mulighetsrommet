import { TilsagnStatus } from "@mr/api-client-v2";
import { Tag } from "@navikt/ds-react";

interface Props {
  status: TilsagnStatus;
}

export function TilsagnTag(props: Props) {
  const { status } = props;

  const baseTagClasses = "min-w-[140px] text-center whitespace-nowrap";

  switch (status) {
    case TilsagnStatus.TIL_GODKJENNING:
      return (
        <Tag size="small" variant="warning" className={baseTagClasses}>
          Til godkjenning
        </Tag>
      );
    case TilsagnStatus.GODKJENT:
      return (
        <Tag size="small" variant="success" className={baseTagClasses}>
          Godkjent
        </Tag>
      );
    case TilsagnStatus.RETURNERT:
      return (
        <Tag size="small" variant="error" className={baseTagClasses}>
          Returnert
        </Tag>
      );
    case TilsagnStatus.TIL_ANNULLERING:
      return (
        <Tag
          size="small"
          variant="neutral"
          className={`${baseTagClasses} bg-white border-[color:var(--a-text-danger)]`}
        >
          Til annullering
        </Tag>
      );
    case TilsagnStatus.ANNULLERT:
      return (
        <Tag
          className={`${baseTagClasses} bg-white text-[color:var(--a-text-danger)] border-[color:var(--a-text-danger)] line-through`}
          size="small"
          variant="neutral"
        >
          Annullert
        </Tag>
      );

    case TilsagnStatus.TIL_OPPGJOR:
      return (
        <Tag
          size="small"
          variant="neutral"
          className={`${baseTagClasses} bg-white border-[color:var(--a-text-danger)]`}
        >
          Til oppgjør
        </Tag>
      );
    case TilsagnStatus.OPPGJORT:
      return (
        <Tag size="small" variant="neutral" className={`${baseTagClasses}`}>
          Oppgjort
        </Tag>
      );
  }
}
