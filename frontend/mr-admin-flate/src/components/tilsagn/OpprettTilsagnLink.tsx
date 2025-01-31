import { TilsagnDefaultsRequest } from "@mr/api-client-v2";
import { Link } from "react-router";

interface Props {
  defaults: TilsagnDefaultsRequest;
}

export function OpprettTilsagnLink({ defaults }: Props) {
  return (
    <Link
      to={
        `/gjennomforinger/${defaults.gjennomforingId}/tilsagn/opprett-tilsagn` +
        `?type=${defaults.type}` +
        `&prismodell=${defaults.prismodell}` +
        `&belop=${defaults.belop}` +
        `&periodeStart=${defaults.periodeStart}` +
        `&periodeSlutt=${defaults.periodeSlutt}` +
        `&kostnadssted=${defaults.kostnadssted}`
      }
    >
      Opprett ekstratilsagn
    </Link>
  );
}
