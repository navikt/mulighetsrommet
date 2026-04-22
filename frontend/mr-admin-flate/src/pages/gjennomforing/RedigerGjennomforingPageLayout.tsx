import { Header } from "@/components/detaljside/Header";
import { FormButtons } from "@/components/skjema/FormButtons";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { DataElementStatusTag } from "@mr/frontend-common";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { Heading } from "@navikt/ds-react";
import { ReactNode, SubmitEventHandler } from "react";
import { FormProvider, UseFormReturn } from "react-hook-form";
import { useGjennomforingByPathParam } from "@/api/gjennomforing/useGjennomforing";
import { GjennomforingFormValues } from "./gjennomforingFormUtils";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { useHead } from "@unhead/react";

interface Props {
  seksjonsnavn: string;
  methods: UseFormReturn<GjennomforingFormValues>;
  onSubmit: SubmitEventHandler<HTMLFormElement>;
  children: ReactNode;
}

export function RedigerGjennomforingPageLayout({
  seksjonsnavn,
  methods,
  onSubmit,
  children,
}: Props) {
  const { gjennomforing } = useGjennomforingByPathParam();

  useHead({
    title: `Redigerer gjennomføring | ${gjennomforing.navn}`,
  });

  return (
    <div>
      <Brodsmuler
        brodsmuler={[
          { tittel: "Gjennomføringer", lenke: "/gjennomforinger" },
          { tittel: gjennomforing.navn, lenke: `/gjennomforinger/${gjennomforing.id}` },
          { tittel: `Rediger ${seksjonsnavn}` },
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
        <InlineErrorBoundary>
          <FormProvider {...methods}>
            <form onSubmit={onSubmit}>
              <FormButtons heading={`Redigerer ${seksjonsnavn}`} />
              <Separator />
              {children}
              <Separator />
              <FormButtons />
            </form>
          </FormProvider>
        </InlineErrorBoundary>
      </WhitePaddedBox>
    </div>
  );
}
