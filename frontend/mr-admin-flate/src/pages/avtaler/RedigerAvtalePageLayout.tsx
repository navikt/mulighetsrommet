import { useAvtale } from "@/api/avtaler/useAvtale";
import { Header } from "@/components/detaljside/Header";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { DataElementStatusTag } from "@mr/frontend-common";
import { Heading } from "@navikt/ds-react";
import { ReactNode } from "react";
import { useHead } from "@unhead/react";
import { InlineErrorBoundary } from "@/ErrorBoundary";

interface Props {
  children: ReactNode;
}

export function RedigerAvtalePageLayout({ children }: Props) {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);

  useHead({
    title: `Redigerer avtale | ${avtale.navn}`,
  });

  return (
    <div data-testid="avtale-form-page">
      <Brodsmuler
        brodsmuler={[
          { tittel: "Avtaler", lenke: "/avtaler" },
          { tittel: "Avtale", lenke: `/avtaler/${avtaleId}` },
          { tittel: "Rediger avtale" },
        ]}
      />
      <Header>
        <AvtaleIkon />
        <Heading size="large" level="2">
          {avtale.navn}
        </Heading>
        <DataElementStatusTag {...avtale.status.status} />
      </Header>
      <WhitePaddedBox>
        <InlineErrorBoundary>{children}</InlineErrorBoundary>
      </WhitePaddedBox>
    </div>
  );
}
