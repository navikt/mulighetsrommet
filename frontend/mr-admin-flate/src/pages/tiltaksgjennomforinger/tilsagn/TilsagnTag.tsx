import { TilsagnDto, TilsagnBesluttelseStatus } from "@mr/api-client";
import { Tag } from "@navikt/ds-react";

export function TilsagnTag(props: { tilsagn: TilsagnDto }) {
  const { tilsagn } = props;

  if (tilsagn?.besluttelse?.status === TilsagnBesluttelseStatus.GODKJENT) {
    return (
      <Tag variant="success" size="small">
        Godkjent
      </Tag>
    );
  } else if (tilsagn?.besluttelse?.status === TilsagnBesluttelseStatus.AVVIST) {
    return (
      <Tag variant="warning" size="small">
        Returnert
      </Tag>
    );
  } else if (tilsagn?.annullertTidspunkt) {
    return (
      <Tag variant="neutral" size="small">
        Annullert
      </Tag>
    );
  } else {
    return (
      <Tag variant="info" size="small">
        Til beslutning
      </Tag>
    );
  }
}
