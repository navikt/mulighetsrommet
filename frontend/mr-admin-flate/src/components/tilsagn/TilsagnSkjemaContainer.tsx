import { TilsagnDto, TiltaksgjennomforingDto } from "@mr/api-client";
import { Location, useLocation, useNavigate } from "react-router-dom";
import { RedigerTilsagn } from "@/components/tilsagn/RedigerTilsagn";
import { OpprettTilsagn } from "@/components/tilsagn/OpprettTilsagn";
import { OpprettEkstratilsagn } from "@/components/tilsagn/OpprettEkstratilsagn";

interface Props {
  tiltaksgjennomforing: TiltaksgjennomforingDto;
  tilsagn?: TilsagnDto;
}

export function TilsagnSkjemaContainer({ tiltaksgjennomforing, tilsagn }: Props) {
  const navigate = useNavigate();

  const location = useLocation() as Location<{ ekstratilsagn?: boolean }>;

  const erEkstratilsagn = location.state?.ekstratilsagn ?? false;

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
  ) : erEkstratilsagn ? (
    <OpprettEkstratilsagn {...props} />
  ) : (
    <OpprettTilsagn {...props} />
  );
}
