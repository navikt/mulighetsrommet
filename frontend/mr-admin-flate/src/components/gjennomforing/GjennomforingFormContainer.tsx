import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { useUpsertGjennomforing } from "@/api/gjennomforing/useUpsertGjennomforing";
import { Laster } from "@/components/laster/Laster";
import {
  InferredGjennomforingSchema,
  GjennomforingSchema,
} from "@/components/redaksjoneltInnhold/GjennomforingSchema";
import { logEvent } from "@/logging/amplitude";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  AvtaleDto,
  GjennomforingDto,
  GjennomforingRequest,
  Tiltakskode,
  ValidationErrorResponse,
} from "@mr/api-client";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { isValidationError } from "@mr/frontend-common/utils/utils";
import { Box, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import React, { useCallback } from "react";
import { DeepPartial, FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import { Separator } from "../detaljside/Metadata";
import { TabWithErrorBorder } from "../skjema/TabWithErrorBorder";
import { GjennomforingRedaksjoneltInnholdForm } from "./GjennomforingRedaksjoneltInnholdForm";
import { GjennomforingFormDetaljer } from "./GjennomforingFormDetaljer";
import { GjennomforingFormKnapperad } from "./GjennomforingFormKnapperad";

interface Props {
  onClose: () => void;
  onSuccess: (id: string) => void;
  avtale: AvtaleDto;
  gjennomforing?: GjennomforingDto;
  defaultValues: DeepPartial<InferredGjennomforingSchema>;
}

function loggRedaktorEndrerTilgjengeligForArrangor(datoValgt: string) {
  logEvent({
    name: "tiltaksadministrasjon.sett-tilgjengelig-for-redaktor",
    data: {
      datoValgt,
    },
  });
}

export function GjennomforingFormContainer({
  avtale,
  gjennomforing,
  defaultValues,
  onClose,
  onSuccess,
}: Props) {
  const redigeringsModus = !!gjennomforing;
  const mutation = useUpsertGjennomforing();
  const [activeTab, setActiveTab] = useAtom(gjennomforingDetaljerTabAtom);

  const form = useForm<InferredGjennomforingSchema>({
    resolver: zodResolver(GjennomforingSchema),
    defaultValues,
  });

  const {
    handleSubmit,
    formState: { errors },
  } = form;

  const handleSuccess = useCallback((dto: GjennomforingDto) => onSuccess(dto.id), [onSuccess]);
  const handleValidationError = useCallback(
    (validation: ValidationErrorResponse) => {
      validation.errors.forEach((error) => {
        const name = mapErrorToSchemaPropertyName(error.name);
        form.setError(name, { type: "custom", message: error.message });
      });

      function mapErrorToSchemaPropertyName(name: string) {
        const mapping: { [name: string]: string } = {
          startDato: "startOgSluttDato.startDato",
          sluttDato: "startOgSluttDato.sluttDato",
          arrangorOrganisasjonsnummer: "tiltaksArrangorUnderenhetOrganisasjonsnummer",
          utdanningslop: "utdanningslop.utdanninger",
        };
        return (mapping[name] ?? name) as keyof InferredGjennomforingSchema;
      }
    },
    [form],
  );

  const postData: SubmitHandler<InferredGjennomforingSchema> = async (data): Promise<void> => {
    const body: GjennomforingRequest = {
      id: gjennomforing?.id || uuidv4(),
      antallPlasser: data.antallPlasser,
      tiltakstypeId: avtale.tiltakstype.id,
      navRegion: data.navRegion,
      navEnheter: data.navEnheter,
      navn: data.navn,
      startDato: data.startOgSluttDato.startDato,
      sluttDato: data.startOgSluttDato.sluttDato ?? null,
      avtaleId: avtale.id,
      administratorer: data.administratorer,
      arrangorId: data.arrangorId,
      oppstart: data.oppstart,
      kontaktpersoner:
        data.kontaktpersoner
          ?.filter((kontakt) => kontakt.navIdent !== null)
          ?.map((kontakt) => ({
            navIdent: kontakt.navIdent!,
            navEnheter: kontakt.navEnheter,
            beskrivelse: kontakt.beskrivelse ?? null,
          })) || [],
      stedForGjennomforing: data.stedForGjennomforing,
      arrangorKontaktpersoner: data.arrangorKontaktpersoner,
      beskrivelse: data.beskrivelse,
      faneinnhold: data.faneinnhold ?? null,
      deltidsprosent: data.deltidsprosent,
      estimertVentetid: data.estimertVentetid ?? null,
      tilgjengeligForArrangorFraOgMedDato: data.tilgjengeligForArrangorFraOgMedDato ?? null,
      amoKategorisering: data.amoKategorisering ?? null,
      utdanningslop:
        avtale.tiltakstype.tiltakskode === Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING
          ? (data.utdanningslop ?? null)
          : null,
    };

    if (
      data.tilgjengeligForArrangorFraOgMedDato &&
      data.startOgSluttDato.startDato !== data.tilgjengeligForArrangorFraOgMedDato
    ) {
      loggRedaktorEndrerTilgjengeligForArrangor(data.tilgjengeligForArrangorFraOgMedDato);
    }

    mutation.mutate(body, {
      onSuccess: handleSuccess,
      onError: (error) => {
        if (isValidationError(error.body)) {
          handleValidationError(error.body);
        }
      },
    });
  };

  const hasRedaksjoneltInnholdErrors = Boolean(errors?.faneinnhold);
  const hasDetaljerErrors = Object.keys(errors).length > (hasRedaksjoneltInnholdErrors ? 1 : 0);

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <Tabs defaultValue={activeTab}>
          <Tabs.List className="flex flex-row justify-between">
            <div>
              <TabWithErrorBorder
                onClick={() => setActiveTab("detaljer")}
                value="detaljer"
                label="Detaljer"
                hasError={hasDetaljerErrors}
              />
              <TabWithErrorBorder
                onClick={() => setActiveTab("redaksjonelt-innhold")}
                value="redaksjonelt-innhold"
                label="Redaksjonelt innhold"
                hasError={hasRedaksjoneltInnholdErrors}
              />
            </div>
            <GjennomforingFormKnapperad
              redigeringsModus={redigeringsModus}
              onClose={onClose}
              mutation={mutation}
            />
          </Tabs.List>
          <Tabs.Panel value="detaljer">
            <InlineErrorBoundary>
              <React.Suspense fallback={<Laster tekst="Laster innhold" />}>
                <Box marginBlock="4">
                  <GjennomforingFormDetaljer avtale={avtale} gjennomforing={gjennomforing} />
                </Box>
              </React.Suspense>
            </InlineErrorBoundary>
          </Tabs.Panel>
          <Tabs.Panel value="redaksjonelt-innhold">
            <Box marginBlock="4">
              <GjennomforingRedaksjoneltInnholdForm avtale={avtale} />
            </Box>
          </Tabs.Panel>
        </Tabs>
        <Separator />
        <GjennomforingFormKnapperad
          redigeringsModus={redigeringsModus}
          onClose={onClose}
          mutation={mutation}
        />
      </form>
    </FormProvider>
  );
}
