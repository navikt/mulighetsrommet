import { capitalizeFirstLetter, joinWithCommaAndOg, tilsagnAarsakTilTekst } from "@/utils/Utils";
import { TilsagnAvvisningAarsak, TilsagnStatus, TotrinnskontrollDto } from "@mr/api-client-v2";
import { Tag } from "@navikt/ds-react";

interface Props {
  status: TilsagnStatus;
  visAarsakerOgForklaring?: boolean;
  annullering?: TotrinnskontrollDto;
  oppgjor?: TotrinnskontrollDto;
}

export function TilsagnTag(props: Props) {
  const { status, annullering, oppgjor, visAarsakerOgForklaring = false } = props;

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
      return (
        <div className={visAarsakerOgForklaring ? "flex flex-col gap-2 items-start" : ""}>
          <Tag
            className={`${baseTagClasses} bg-white text-[color:var(--a-text-danger)] border-[color:var(--a-text-danger)]`}
            size="small"
            variant="neutral"
          >
            Annullert
          </Tag>
          {visAarsakerOgForklaring ? (
            <VisAarsakerOgForklaring
              type="Tilsagnet"
              status="annullert"
              aarsaker={
                annullering?.aarsaker?.map((aarsak) =>
                  tilsagnAarsakTilTekst(aarsak as TilsagnAvvisningAarsak),
                ) || []
              }
              forklaring={annullering?.forklaring}
            />
          ) : null}
        </div>
      );
    }
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
        <div className={visAarsakerOgForklaring ? "flex flex-col gap-2 items-start" : ""}>
          <Tag
            size="small"
            variant="neutral"
            className={`${baseTagClasses} bg-white border-[color:var(--a-text-danger)]`}
          >
            Oppgjort
          </Tag>
          {visAarsakerOgForklaring ? (
            <VisAarsakerOgForklaring
              type="Tilsagnet"
              status="gjort opp"
              aarsaker={
                oppgjor?.aarsaker?.map((aarsak) =>
                  tilsagnAarsakTilTekst(aarsak as TilsagnAvvisningAarsak),
                ) || []
              }
              forklaring={oppgjor?.forklaring}
            />
          ) : null}
        </div>
      );
  }
}

function VisAarsakerOgForklaring({
  aarsaker,
  forklaring,
  type,
  status,
}: {
  aarsaker: string[];
  forklaring?: string;
  type: "Tilsagnet";
  status: "annullert" | "gjort opp";
}) {
  return (
    <p className="prose text-balance">
      {type} ble {status} med følgende {aarsaker.length > 1 ? "årsaker: " : "årsak: "}
      <b>{capitalizeFirstLetter(joinWithCommaAndOg(aarsaker))}</b>
      {forklaring ? (
        <>
          {" "}
          med forklaring: <i>"{forklaring}"</i>
        </>
      ) : null}
      .
    </p>
  );
}
