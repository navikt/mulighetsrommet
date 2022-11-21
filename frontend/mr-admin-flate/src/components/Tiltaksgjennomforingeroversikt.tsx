import { Tiltaksgjennomforingrad } from "./Tiltakstype";
import { useTiltaksgjennomforing } from "../api/tiltaksgjennomforing/useTiltaksgjennomforing";

export function Tiltaksgjennomforingeroversikt() {
  const { data, isLoading } = useTiltaksgjennomforing();
  if (isLoading) {
    return null;
  }
  if (!data) {
    return null;
  }
  const { data: tiltaksgjennomforinger, pagination: paginering } = data;

  return (
    <ul>
      {tiltaksgjennomforinger.map((tiltaksgjennomforing) => (
        <Tiltaksgjennomforingrad key={tiltaksgjennomforing.id} tiltaksgjennomforing={tiltaksgjennomforing} />
      ))}
    </ul>
  );
}
