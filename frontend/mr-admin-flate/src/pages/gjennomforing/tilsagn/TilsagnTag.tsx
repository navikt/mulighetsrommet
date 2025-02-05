import { TilsagnStatus, TilsagnTilAnnulleringAarsak, ToTrinnskontroll } from "@mr/api-client-v2";
import { BodyLong, List, Tag, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { tilsagnAarsakTilTekst } from "@/utils/Utils";

interface Props {
  status: TilsagnStatus;
  annullering?: ToTrinnskontroll;
  expandable?: boolean;
}

export function TilsagnTag(props: Props) {
  const { status, annullering, expandable = false } = props;
  const [expandLabel, setExpandLabel] = useState<boolean>(false);

  const baseTagClasses = "min-w-[140px] text-center whitespace-nowrap";

  switch (status) {
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
          className={`${baseTagClasses} bg-white border-[color:var(--a-text-danger)]`}
        >
          Til annullering
        </Tag>
      );
    case TilsagnStatus.ANNULLERT: {
      const annullertLabel = expandable ? "Annullert..." : "Annullert";
      return (
        <Tag
          className={`${baseTagClasses} line-through hover:no-underline bg-white text-[color:var(--a-text-danger)] border-[color:var(--a-text-danger)]`}
          size="small"
          onMouseEnter={() => setExpandLabel(true)}
          onMouseLeave={() => setExpandLabel(false)}
          variant="neutral"
        >
          {expandable && expandLabel ? (
            <ExpandedAnnullert annullering={annullering!} />
          ) : (
            truncate(annullertLabel, 30)
          )}
        </Tag>
      );
    }
  }
}

function ExpandedAnnullert({ annullering }: { annullering: ToTrinnskontroll }) {
  return (
    <VStack>
      <List
        as="ul"
        size="small"
        title="Årsaker"
        className="[&>li]:pl-2 [&>li]:ml-2 [&>li]:marker:content-['\2022'] [&>li]:marker:text-[color:var(--a-text-danger)] [&>li]:marker:text-lg"
      >
        {annullering.aarsaker?.map((aarsak) => (
          <li>{tilsagnAarsakTilTekst(aarsak as TilsagnTilAnnulleringAarsak)}</li>
        ))}
      </List>
      {annullering.forklaring && <BodyLong>{annullering.forklaring}</BodyLong>}
    </VStack>
  );
}

function truncate(text: string, maxLength: number): string {
  return text.length > maxLength ? `${text.substring(0, maxLength - 3)}...` : text;
}
