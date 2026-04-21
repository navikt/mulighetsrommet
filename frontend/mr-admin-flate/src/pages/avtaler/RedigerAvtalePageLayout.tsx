import { useAvtale } from "@/api/avtaler/useAvtale";
import { Header } from "@/components/detaljside/Header";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { DataElementStatusTag } from "@mr/frontend-common";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { Heading } from "@navikt/ds-react";
import { AvtaleFormValues } from "@/schemas/avtale";
import { ReactNode, SubmitEventHandler } from "react";
import { FormProvider, UseFormReturn } from "react-hook-form";
import { FormButtons } from "@/components/skjema/FormButtons";
import { useHead } from "@unhead/react";
import { InlineErrorBoundary } from "@/ErrorBoundary";

interface Props {
  seksjonsnavn: string;
  methods: UseFormReturn<AvtaleFormValues>;
  onSubmit: SubmitEventHandler<HTMLFormElement>;
  children: ReactNode;
}

export function RedigerAvtalePageLayout({ seksjonsnavn, methods, onSubmit, children }: Props) {
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
          { tittel: `Rediger ${seksjonsnavn}` },
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
