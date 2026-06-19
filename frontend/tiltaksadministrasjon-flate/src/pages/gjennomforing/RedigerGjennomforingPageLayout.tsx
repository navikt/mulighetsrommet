import { GjennomforingAvtaleIkon } from "@/components/ikoner/GjennomforingAvtaleIkon";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { ReactNode } from "react";
import { useGjennomforingByPathParam } from "@/api/gjennomforing/useGjennomforing";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { useHead } from "@unhead/react";
import { HeaderBanner } from "@/layouts/HeaderBanner";

interface Props {
  children: ReactNode;
}

export function RedigerGjennomforingPageLayout({ children }: Props) {
  const { gjennomforing } = useGjennomforingByPathParam();

  useHead({
    title: `Redigerer gjennomføring | ${gjennomforing.navn}`,
  });

  return (
    <div>
      <Brodsmuler
        brodsmuler={[
          { tittel: "Gjennomføringer", lenke: "/gjennomforinger" },
          { tittel: "Gjennomføring", lenke: `/gjennomforinger/${gjennomforing.id}` },
          { tittel: `Rediger gjennomføring` },
        ]}
      />
      <HeaderBanner
        ikon={<GjennomforingAvtaleIkon />}
        heading={gjennomforing.navn}
        status={gjennomforing.status.status}
      />
      <WhitePaddedBox>
        <InlineErrorBoundary>{children}</InlineErrorBoundary>
      </WhitePaddedBox>
    </div>
  );
}
