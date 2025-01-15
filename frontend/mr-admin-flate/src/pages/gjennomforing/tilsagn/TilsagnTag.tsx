import { TilsagnStatus, TilsagnStatusAnnullert, TilsagnStatusDto } from "@mr/api-client";
import { BodyLong, List, Tag, VStack } from "@navikt/ds-react";
import styles from "./TilsagnTag.module.scss";
import { useState } from "react";
import { tilsagnAarsakTilTekst } from "@/utils/Utils";

interface Props {
  status: TilsagnStatusDto;
  expandable?: boolean;
}

export function TilsagnTag(props: Props) {
  const { status, expandable = false } = props;
  const [expandLabel, setExpandLabel] = useState<boolean>(false);

  const baseTagClasses = "w-[140px] text-center whitespace-nowrap";

  switch (status.type) {
    case TilsagnStatus.TIL_GODKJENNING:
      return (
        <Tag size="small" variant="alt1" className={baseTagClasses}>
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
          className={`${baseTagClasses} ${styles.til_annullering_tag}`}
        >
          Til annullering
        </Tag>
      );
    case TilsagnStatus.ANNULLERT: {
      const annullertLabel = expandable ? "Annullert..." : "Annullert";
      return (
        <Tag
          className={`${baseTagClasses} ${styles.annullert_tag}`}
          size="small"
          onMouseEnter={() => setExpandLabel(true)}
          onMouseLeave={() => setExpandLabel(false)}
          variant="neutral"
          style={{
            maxWidth: expandable ? "400px" : "140px",
          }}
        >
          {expandable && expandLabel ? (
            <ExpandedAnnullert status={status} />
          ) : (
            truncate(annullertLabel, 30)
          )}
        </Tag>
      );
    }
  }
}

function ExpandedAnnullert({ status }: { status: TilsagnStatusAnnullert }) {
  return (
    <VStack>
      <List as="ul" size="small" title="Årsaker" className={styles.annullert_aarsak_list}>
        {status.aarsaker.map((aarsak) => (
          <li>{tilsagnAarsakTilTekst(aarsak)}</li>
        ))}
      </List>
      {status.forklaring && <BodyLong>{status.forklaring}</BodyLong>}
    </VStack>
  );
}

function truncate(text: string, maxLength: number): string {
  return text.length > maxLength ? `${text.substring(0, maxLength - 3)}...` : text;
}
