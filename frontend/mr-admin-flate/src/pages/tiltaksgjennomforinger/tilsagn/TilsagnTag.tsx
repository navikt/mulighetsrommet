import { TilsagnDto, TilsagnStatus } from "@mr/api-client";
import { Tag } from "@navikt/ds-react";
import styles from "./TilsagnTag.module.scss";

export function TilsagnTag(props: { tilsagn: TilsagnDto }) {
  const { tilsagn } = props;
  const { status } = tilsagn;

  switch (status) {
    case TilsagnStatus.GODKJENT:
      return tilsagn.besluttelse ? <Tag variant="success">Godkjent</Tag> : null;
    case TilsagnStatus.RETURNERT:
      return tilsagn.besluttelse ? <Tag variant="error">Returnert</Tag> : null;
    case TilsagnStatus.OPPGJORT:
      return <Tag variant="neutral">Oppgjort</Tag>;
    case TilsagnStatus.ANNULLERT:
      return (
        <Tag variant="neutral" className={styles.annullert_tag}>
          Annullert
        </Tag>
      );
    case TilsagnStatus.TIL_GODKJENNING:
      return <Tag variant="alt1">Til godkjenning</Tag>;
  }
}
