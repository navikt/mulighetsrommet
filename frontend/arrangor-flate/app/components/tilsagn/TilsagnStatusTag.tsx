import { TilsagnStatus } from "api-client";
import { Tag } from "@navikt/ds-react";

interface Props {
  status: TilsagnStatus;
}

export function TilsagnStatusTag({ status }: Props) {
  switch (status) {
    case TilsagnStatus.RETURNERT:
    case TilsagnStatus.TIL_GODKJENNING:
      return null;
    case TilsagnStatus.GODKJENT:
      return <Tag variant="success">Godkjent</Tag>;
    case TilsagnStatus.TIL_ANNULLERING:
      return <Tag variant="warning">Til annullering</Tag>;
    case TilsagnStatus.ANNULLERT:
      return (
        <Tag variant="error" className="line-through bg-white! text-text-danger!">
          {" "}
          Annullert{" "}
        </Tag>
      );
    case TilsagnStatus.TIL_OPPGJOR:
      return <Tag variant="warning">Til oppgjør</Tag>;
    case TilsagnStatus.OPPGJORT:
      return <Tag variant="neutral">Oppgjort</Tag>;
  }
}
