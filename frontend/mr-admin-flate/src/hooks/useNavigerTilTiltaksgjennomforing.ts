export const useNavigerTilTiltaksgjennomforing = () => {
  const navigerTilTiltaksgjennomforing = (tiltaksgjennomforingId: String) => {
    const origin = window.location.origin;
    window.location.href = `${origin}/tiltaksgjennomforinger/${tiltaksgjennomforingId}`;
  };
  return { navigerTilTiltaksgjennomforing };
}