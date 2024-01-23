import { InternalHeader, Spacer } from "@navikt/ds-react";

export const PreviewHeader = () => {
  return (
    <header>
      <InternalHeader>
        <InternalHeader.Title href="/preview">Arbeidsmarkedstiltak</InternalHeader.Title>
        <Spacer />
      </InternalHeader>
    </header>
  );
};
