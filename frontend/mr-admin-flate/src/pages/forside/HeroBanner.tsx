import { Heading } from "@navikt/ds-react";

export function HeroBanner() {
  return (
    <Heading size="large" level="2" className="text-white p-0 m-0" data-testid="heading">
      Enkel og effektiv administrasjon
      <br /> av arbeidsmarkedstiltak
    </Heading>
  );
}
