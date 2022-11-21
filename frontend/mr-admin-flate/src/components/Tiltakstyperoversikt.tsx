import { useTiltakstyper } from "../api/tiltakstyper/useTiltakstyper";
import { Tiltakstyperad } from "./Tiltakstype";

export function Tiltakstyperoversikt() {
  const { data, isLoading } = useTiltakstyper();
  if (isLoading) {
    return null;
  }
  if (!data) {
    return null;
  }
  const { data: tiltakstyper, pagination: paginering } = data;

  return (
    <ul>
      {tiltakstyper.map((tiltakstype) => (
        <Tiltakstyperad key={tiltakstype.id} tiltakstype={tiltakstype} />
      ))}
    </ul>
  );
}
