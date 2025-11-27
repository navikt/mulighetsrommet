import { useAvtale } from "@/api/avtaler/useAvtale";
import { Header } from "@/components/detaljside/Header";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Heading } from "@navikt/ds-react";
import { DataElementStatusTag } from "@mr/frontend-common";
import { Outlet, useLocation, useNavigate } from "react-router";
import { AvtaleFormKnapperad } from "@/components/avtaler/AvtaleFormKnapperad";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { useUpsertDetaljer } from "@/api/avtaler/useUpsertDetaljer";
import { useUpsertPersonvern } from "@/api/avtaler/useUpsertPersonvern";
import { useUpsertVeilederinformasjon } from "@/api/avtaler/useUpsertVeilederinformasjon";
import {
  AvtaleFormInput,
  avtaleFormSchema,
  AvtaleFormValues,
  defaultAvtaleData,
} from "@/schemas/avtale";
import {
  toVeilederinfoRequest,
  toPersonvernRequest,
  toDetaljerRequest,
  mapNameToSchemaPropertyName,
} from "./avtaleFormUtils";
import { FormProvider, useForm } from "react-hook-form";
import { useQueryClient } from "@tanstack/react-query";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { zodResolver } from "@hookform/resolvers/zod";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { AvtaleDto, ValidationError } from "@tiltaksadministrasjon/api-client";
import { useCallback } from "react";
import { QueryKeys } from "@/api/QueryKeys";

function brodsmuler(avtaleId: string): Array<Brodsmule | undefined> {
  return [
    {
      tittel: "Avtaler",
      lenke: "/avtaler",
    },
    {
      tittel: "Avtale",
      lenke: `/avtaler/${avtaleId}`,
    },
    {
      tittel: "Rediger avtale",
    },
  ];
}

function redigeringstittel(pathname: string): string {
  if (pathname.includes("veilederinformasjon")) {
    return "Redigerer veilederinformasjon";
  } else if (pathname.includes("personvern")) {
    return "Redigerer personvern";
  } else {
    return "Redigerer avtaledetaljer";
  }
}

export function AvtaleFormPage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const navigate = useNavigate();
  const { data: avtale } = useAvtale(avtaleId);
  const { pathname } = useLocation();
  const queryClient = useQueryClient();
  const { data: ansatt } = useHentAnsatt();

  const methods = useForm<AvtaleFormInput, any, AvtaleFormValues>({
    resolver: zodResolver(avtaleFormSchema),
    defaultValues: defaultAvtaleData(ansatt, avtale),
  });

  const handleValidationError = useCallback(
    (validation: ValidationError) => {
      validation.errors.forEach((error) => {
        const name = mapNameToSchemaPropertyName(jsonPointerToFieldPath(error.pointer));
        methods.setError(name, { type: "custom", message: error.detail });
      });
    },
    [methods],
  );

  const detaljerMutation = useUpsertDetaljer(avtaleId);
  const personvernMutation = useUpsertPersonvern(avtaleId);
  const veilederinfoMutation = useUpsertVeilederinformasjon(avtaleId);

  const onSubmit = async (data: AvtaleFormValues) => {
    let mutation;
    let request;

    if (pathname.includes("veilederinformasjon")) {
      mutation = veilederinfoMutation;
      request = toVeilederinfoRequest({ data, id: avtaleId });
    } else if (pathname.includes("personvern")) {
      mutation = personvernMutation;
      request = toPersonvernRequest({ data, id: avtaleId });
    } else {
      mutation = detaljerMutation;
      request = toDetaljerRequest({ data, id: avtaleId });
    }

    (mutation as { mutate: (request: unknown, options: any) => void }).mutate(request, {
      onValidationError: handleValidationError,
      onSuccess: (dto: { data: AvtaleDto }) => {
        queryClient.setQueryData(QueryKeys.avtale(dto.data.id), dto.data);
        navigate(pathname.replace("/skjema", ""));
      },
    });
  };

  return (
    <div data-testid="avtale-form-page">
      <title>{`Redigerer avtale | ${avtale.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler(avtaleId)} />
      <Header>
        <AvtaleIkon />
        <Heading size="large" level="2">
          {avtale.navn}
        </Heading>
        <DataElementStatusTag {...avtale.status.status} />
      </Header>
      <ContentBox>
        <WhitePaddedBox>
          <FormProvider {...methods}>
            <form onSubmit={methods.handleSubmit(onSubmit)}>
              <AvtaleFormKnapperad heading={redigeringstittel(pathname)} />
              <Separator />
              <Outlet />
              <Separator />
              <AvtaleFormKnapperad />
            </form>
          </FormProvider>
        </WhitePaddedBox>
      </ContentBox>
    </div>
  );
}
