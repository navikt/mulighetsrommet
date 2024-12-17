import { TilsagnStatus } from "@mr/api-client";
import { Tag } from "@navikt/ds-react";
import styles from "./TilsagnTag.module.scss";

export function TilsagnTag(props: { status: TilsagnStatus }) {
  const { status } = props;

  switch (status.type) {
    case "TIL_GODKJENNING":
      return <Tag variant="alt1">Til godkjenning</Tag>;
    case "GODKJENT":
      return <Tag variant="success">Godkjent</Tag>;
    case "RETURNERT":
      return <Tag variant="error">Returnert</Tag>;
    case "TIL_ANNULLERING":
      return (
        <Tag variant="neutral" className={styles.til_annullering_tag}>
          Til annullering
        </Tag>
      );
    case "ANNULLERT":
      return (
        <Tag variant="neutral" className={styles.annullert_tag}>
          Annullert
        </Tag>
      );
  }
}
