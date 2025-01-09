import { TilsagnStatus } from "@mr/api-client-v2";
import { Tag } from "@navikt/ds-react";

interface Props {
  status: TilsagnStatus;
}

export function TilsagnStatusTag({ status }: Props) {
  switch (status) {
    case TilsagnStatus.GODKJENT:
      return <Tag variant="success">Godkjent</Tag>;
    case TilsagnStatus.ANNULLERT:
      return (
        <Tag variant="error" className="line-through bg-white text-text-danger border-text-danger">
          Annullert
        </Tag>
      );
    case TilsagnStatus.TIL_ANNULLERING:
      return (
        <Tag variant="neutral" className="bg-white  border-text-danger">
          Til annullering
        </Tag>
      );
    default:
      return null;
  }
}
