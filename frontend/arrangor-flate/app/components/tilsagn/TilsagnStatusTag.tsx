import { StatusTag } from "@mr/frontend-common";
import { TilsagnStatus } from "api-client";

export function TilsagnStatusTag({ status }: { status: TilsagnStatus }) {
  switch (status) {
    case TilsagnStatus.RETURNERT:
    case TilsagnStatus.TIL_GODKJENNING:
      return null;
    case TilsagnStatus.GODKJENT:
      return <StatusTag variant="success">Godkjent</StatusTag>;
    case TilsagnStatus.TIL_ANNULLERING:
      return <StatusTag variant="warning">Til annullering</StatusTag>;
    case TilsagnStatus.ANNULLERT:
      return (
        <StatusTag
          variant="neutral"
          className={
            "bg-white text-[color:var(--a-text-danger)] border-[color:var(--a-text-danger)] line-through"
          }
        >
          Annullert
        </StatusTag>
      );
    case TilsagnStatus.TIL_OPPGJOR:
      return <StatusTag variant="warning">Til oppgjør</StatusTag>;
    case TilsagnStatus.OPPGJORT:
      return <StatusTag variant="neutral">Oppgjort</StatusTag>;
  }
}
