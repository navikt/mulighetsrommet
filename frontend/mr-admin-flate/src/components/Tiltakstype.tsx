import { Tiltakstype } from "../../../mulighetsrommet-api-client";

interface Props {
  tiltakstype: Tiltakstype;
}

export function Tiltakstyperad({ tiltakstype }: Props) {
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "3fr 1fr 1fr",
      }}
    >
      <div>{tiltakstype.navn}</div>
      <div>{tiltakstype.tiltakskode}</div>
      <div>{tiltakstype.innsatsgruppe}</div>
    </div>
  );
}
