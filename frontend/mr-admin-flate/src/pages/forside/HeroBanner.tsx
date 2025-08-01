import { Heading } from "@navikt/ds-react";

export function HeroBanner() {
  return (
    <div className="bg-[var(--a-deepblue-800)] p-16 min-h-[15rem] w-full mb-6">
      <Heading size="large" level="2" className="text-white p-0 m-0" data-testid="heading">
        Enkel og effektiv administrasjon
        <br /> av arbeidsmarkedstiltak
      </Heading>
    </div>
  );
}
