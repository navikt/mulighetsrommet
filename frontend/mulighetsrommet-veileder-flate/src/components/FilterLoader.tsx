import { Loader } from "@navikt/ds-react";

export function FilterLoader() {
  return (
    <div style={{ display: "block", margin: "0 auto", textAlign: "center" }}>
      <Loader size="xlarge" title="Laster tiltaksgjennomføring..." />
    </div>
  );
}
