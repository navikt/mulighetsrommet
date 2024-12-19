import { TilsagnDto, TilsagnType, TiltaksgjennomforingDto } from "@mr/api-client";
import { Location, useLocation, useNavigate } from "react-router-dom";
import { RedigerTilsagn } from "@/components/tilsagn/RedigerTilsagn";
import { OpprettTilsagn } from "@/components/tilsagn/OpprettTilsagn";

interface Props {
  tiltaksgjennomforing: TiltaksgjennomforingDto;
  tilsagn?: TilsagnDto;
}

export function TilsagnSkjemaContainer({ tiltaksgjennomforing, tilsagn }: Props) {
  const navigate = useNavigate();

  const location = useLocation() as Location<{ ekstratilsagn?: boolean }>;

  const type = location.state?.ekstratilsagn ? TilsagnType.EKSTRATILSAGN : TilsagnType.TILSAGN;

  function navigerTilTilsagn() {
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/tilsagn`);
  }

  const props = {
    gjennomforing: tiltaksgjennomforing,
    onSuccess: navigerTilTilsagn,
    onAvbryt: navigerTilTilsagn,
  };

  return tilsagn ? (
    <RedigerTilsagn tilsagn={tilsagn} {...props} />
  ) : (
    <OpprettTilsagn type={type} {...props} />
  );
}
