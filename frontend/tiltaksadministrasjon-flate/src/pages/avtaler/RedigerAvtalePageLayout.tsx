import { useAvtale } from "@/api/avtaler/useAvtale";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { ReactNode } from "react";
import { useHead } from "@unhead/react";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { HeaderBanner } from "@/layouts/HeaderBanner";

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
      <HeaderBanner ikon={<AvtaleIkon />} heading={avtale.navn} status={avtale.status.status} />
      <WhitePaddedBox>
        <InlineErrorBoundary>{children}</InlineErrorBoundary>
      </WhitePaddedBox>
    </div>
  );
}
