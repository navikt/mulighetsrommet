import { Loader } from "@navikt/ds-react";

export function TiltakLoader() {
  return (
    <div style={{ display: "block", margin: "0 auto", textAlign: "center" }}>
      <Loader size="xlarge" title="Laster tiltak..." />
    </div>
  );
}
