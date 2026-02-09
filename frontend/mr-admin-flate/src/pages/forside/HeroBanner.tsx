import { Heading } from "@navikt/ds-react";

export function HeroBanner() {
  return (
    <div className="bg-ax-brand-blue-1000 p-16 min-h-60 w-full mb-6">
      <Heading size="large" level="2" className="text-ax-accent-100 p-0 m-0" data-testid="heading">
        Enkel og effektiv administrasjon
        <br /> av arbeidsmarkedstiltak
      </Heading>
    </div>
  );
}
