import { zodResolver } from "@hookform/resolvers/zod";
import { Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  AvtaleDto,
  TiltaksgjennomforingDto,
  TiltaksgjennomforingRequest,
  Tiltakskode,
  ValidationErrorResponse,
} from "@mr/api-client";
import { DeepPartial, FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { useHandleApiUpsertResponse } from "@/api/effects";
import { useUpsertTiltaksgjennomforing } from "@/api/tiltaksgjennomforing/useUpsertTiltaksgjennomforing";
import { Separator } from "../detaljside/Metadata";
import styles from "./TiltaksgjennomforingSkjemaContainer.module.scss";
import { TiltakgjennomforingRedaksjoneltInnholdForm } from "./TiltaksgjennomforingRedaksjoneltInnholdForm";
import {
  InferredTiltaksgjennomforingSchema,
  TiltaksgjennomforingSchema,
} from "@/components/redaksjoneltInnhold/TiltaksgjennomforingSchema";
import { TiltaksgjennomforingSkjemaDetaljer } from "./TiltaksgjennomforingSkjemaDetaljer";
import { TiltaksgjennomforingSkjemaKnapperad } from "./TiltaksgjennomforingSkjemaKnapperad";
import { logEvent } from "@/logging/amplitude";
import { RedaksjoneltInnholdBunnKnapperad } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdBunnKnapperad";
import { TabWithErrorBorder } from "../skjema/TabWithErrorBorder";
import React, { useCallback } from "react";
import { Laster } from "@/components/laster/Laster";
import { InlineErrorBoundary } from "@mr/frontend-common";

interface Props {
  onClose: () => void;
  onSuccess: (id: string) => void;
  avtale: AvtaleDto;
  tiltaksgjennomforing?: TiltaksgjennomforingDto;
  defaultValues: DeepPartial<InferredTiltaksgjennomforingSchema>;
}

function loggRedaktorEndrerTilgjengeligForArrangor(datoValgt: string) {
  logEvent({
    name: "tiltaksadministrasjon.sett-tilgjengelig-for-redaktor",
    data: {
      datoValgt,
    },
  });
}

export function TiltaksgjennomforingSkjemaContainer({
  avtale,
  tiltaksgjennomforing,
  defaultValues,
  onClose,
  onSuccess,
}: Props) {
  const redigeringsModus = !!tiltaksgjennomforing;
  const mutation = useUpsertTiltaksgjennomforing();
  const [activeTab, setActiveTab] = useAtom(gjennomforingDetaljerTabAtom);

  const form = useForm<InferredTiltaksgjennomforingSchema>({
    resolver: zodResolver(TiltaksgjennomforingSchema),
    defaultValues,
  });

  const {
    handleSubmit,
    formState: { errors },
  } = form;

  const handleSuccess = useCallback(
    (dto: TiltaksgjennomforingDto) => onSuccess(dto.id),
    [onSuccess],
  );
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
        return (mapping[name] ?? name) as keyof InferredTiltaksgjennomforingSchema;
      }
    },
    [form],
  );

  useHandleApiUpsertResponse(mutation, handleSuccess, handleValidationError);

  const postData: SubmitHandler<InferredTiltaksgjennomforingSchema> = async (
    data,
  ): Promise<void> => {
    const body: TiltaksgjennomforingRequest = {
      id: tiltaksgjennomforing?.id || uuidv4(),
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
      apentForInnsok: data.apentForInnsok,
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

    mutation.mutate(body);
  };

  const hasRedaksjoneltInnholdErrors = Boolean(errors?.faneinnhold);
  const hasDetaljerErrors = Object.keys(errors).length > (hasRedaksjoneltInnholdErrors ? 1 : 0);

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <Tabs defaultValue={activeTab}>
          <Tabs.List className={styles.tabslist}>
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
            <TiltaksgjennomforingSkjemaKnapperad
              redigeringsModus={redigeringsModus}
              onClose={onClose}
              mutation={mutation}
            />
          </Tabs.List>
          <Tabs.Panel value="detaljer">
            <InlineErrorBoundary>
              <React.Suspense fallback={<Laster tekst="Laster innhold" />}>
                <TiltaksgjennomforingSkjemaDetaljer
                  avtale={avtale}
                  tiltaksgjennomforing={tiltaksgjennomforing}
                />
              </React.Suspense>
            </InlineErrorBoundary>
          </Tabs.Panel>
          <Tabs.Panel value="redaksjonelt-innhold">
            <TiltakgjennomforingRedaksjoneltInnholdForm avtale={avtale} />
          </Tabs.Panel>
        </Tabs>
        <Separator />
        <RedaksjoneltInnholdBunnKnapperad>
          <TiltaksgjennomforingSkjemaKnapperad
            redigeringsModus={redigeringsModus}
            onClose={onClose}
            mutation={mutation}
          />
        </RedaksjoneltInnholdBunnKnapperad>
      </form>
    </FormProvider>
  );
}
