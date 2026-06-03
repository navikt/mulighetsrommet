import { Header } from "@/components/detaljside/Header";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { DataElementStatusTag } from "@mr/frontend-common";
import { Heading } from "@navikt/ds-react";
import { ReactNode } from "react";
import { useGjennomforingByPathParam } from "@/api/gjennomforing/useGjennomforing";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { useHead } from "@unhead/react";

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
      <Header>
        <GjennomforingIkon />
        <Heading size="large" level="2">
          {gjennomforing.navn}
        </Heading>
        <DataElementStatusTag {...gjennomforing.status.status} />
      </Header>
      <WhitePaddedBox>
        <InlineErrorBoundary>{children}</InlineErrorBoundary>
      </WhitePaddedBox>
    </div>
  );
}
