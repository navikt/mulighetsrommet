import { TilsagnDefaultsRequest } from "@mr/api-client-v2";
import { Button } from "@navikt/ds-react";
import { useNavigate } from "react-router";

interface Props {
  defaults: TilsagnDefaultsRequest;
}

export function OpprettTilsagnButton({ defaults }: Props) {
  const navigate = useNavigate();

  return (
    <Button
      size="small"
      type="button"
      variant="primary"
      onClick={() =>
        navigate(
          `/gjennomforinger/${defaults.gjennomforingId}/tilsagn/opprett-tilsagn` +
            `?type=${defaults.type}` +
            `&prismodell=${defaults.prismodell}` +
            `&belop=${defaults.belop}` +
            `&periodeStart=${defaults.periodeStart}` +
            `&periodeSlutt=${defaults.periodeSlutt}` +
            `&kostnadssted=${defaults.kostnadssted}`,
        )
      }
    >
      Opprett ekstratilsagn
    </Button>
  );
}
