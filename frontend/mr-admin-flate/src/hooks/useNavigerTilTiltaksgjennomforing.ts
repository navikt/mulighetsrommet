import { useNavigate } from "react-router-dom";

export const useNavigerTilTiltaksgjennomforing = () => {
  const navigate = useNavigate();
  const navigerTilTiltaksgjennomforing = (tiltaksgjennomforingId: String) => {
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforingId}`);
  };
  return { navigerTilTiltaksgjennomforing };
};
