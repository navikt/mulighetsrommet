import { avtaleDetaljerTabAtom } from "@/api/atoms";
import { useUpsertAvtale } from "@/api/avtaler/useUpsertAvtale";
import { AvtaleSchema, InferredAvtaleSchema } from "@/components/redaksjoneltInnhold/AvtaleSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  AvtaleDto,
  AvtaleRequest,
  EmbeddedTiltakstype,
  NavAnsatt,
  NavEnhet,
  Prismodell,
  Tiltakskode,
  TiltakstypeDto,
  Toggles,
  UtdanningslopDbo,
  ValidationError,
} from "@mr/api-client-v2";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { Box, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import React, { useCallback } from "react";
import { DeepPartial, FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import { Separator } from "../detaljside/Metadata";
import { Laster } from "../laster/Laster";
import { TabWithErrorBorder } from "../skjema/TabWithErrorBorder";
import { AvtalePersonvernForm } from "./AvtalePersonvernForm";
import { AvtaleRedaksjoneltInnholdForm } from "./AvtaleRedaksjoneltInnholdForm";
import { AvtaleFormDetaljer } from "./AvtaleFormDetaljer";
import { AvtaleFormKnapperad } from "./AvtaleFormKnapperad";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { AvtalePrisOgFakturering } from "@/pages/avtaler/AvtalePrisOgFakturering";
import { z } from "zod";

interface Props {
  onClose: () => void;
  onSuccess: (id: string) => void;
  tiltakstyper: TiltakstypeDto[];
  ansatt: NavAnsatt;
  avtale?: AvtaleDto;
  enheter: NavEnhet[];
  redigeringsModus: boolean;
  defaultValues: DeepPartial<InferredAvtaleSchema>;
}

export function AvtaleFormContainer({
  onClose,
  onSuccess,
  ansatt,
  avtale,
  redigeringsModus,
  defaultValues,
  ...props
}: Props) {
  const [activeTab, setActiveTab] = useAtom(avtaleDetaljerTabAtom);

  const mutation = useUpsertAvtale();

  type FormValues = z.infer<typeof AvtaleSchema>;
  const form = useForm<z.input<typeof AvtaleSchema>, any, FormValues>({
    resolver: zodResolver(AvtaleSchema),
    defaultValues,
  });

  const {
    handleSubmit,
    formState: { errors },
    watch,
  } = form;

  const watchedTiltakstype: EmbeddedTiltakstype | undefined = watch("tiltakstype");

  const { data: enableTilsagn } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_TILSAGN,
    watchedTiltakstype ? [watchedTiltakstype.tiltakskode] : [],
  );

  const postData: SubmitHandler<InferredAvtaleSchema> = async (data): Promise<void> => {
    const requestBody: AvtaleRequest = {
      id: avtale?.id ?? uuidv4(),
      navEnheter: data.navEnheter.concat(data.navRegioner),
      avtalenummer: avtale?.avtalenummer || null,
      sakarkivNummer: data.sakarkivNummer || null,
      arrangor:
        data.arrangorHovedenhet && data.arrangorUnderenheter
          ? {
              hovedenhet: data.arrangorHovedenhet,
              underenheter: data.arrangorUnderenheter,
              kontaktpersoner: data.arrangorKontaktpersoner || [],
            }
          : null,
      navn: data.navn,
      sluttDato: data.startOgSluttDato.sluttDato || null,
      startDato: data.startOgSluttDato.startDato,
      tiltakstypeId: data.tiltakstype.id,
      administratorer: data.administratorer,
      avtaletype: data.avtaletype,
      prisbetingelser:
        !data.prismodell || data.prismodell === Prismodell.FRI
          ? data.prisbetingelser || null
          : null,
      beskrivelse: data.beskrivelse,
      faneinnhold: data.faneinnhold,
      personopplysninger: data.personvernBekreftet ? data.personopplysninger : [],
      personvernBekreftet: data.personvernBekreftet,
      amoKategorisering: data.amoKategorisering || null,
      opsjonsmodell: {
        type: data?.opsjonsmodell?.type || null,
        opsjonMaksVarighet: data?.opsjonsmodell?.opsjonMaksVarighet || null,
        customOpsjonsmodellNavn: data?.opsjonsmodell?.customOpsjonsmodellNavn || null,
      },
      utdanningslop: getUtdanningslop(data),
      prismodell: enableTilsagn ? data.prismodell : null,
    };

    mutation.mutate(requestBody, {
      onSuccess: handleSuccess,
      onValidationError: (error: ValidationError) => {
        handleValidationError(error);
      },
    });
  };

  const handleSuccess = useCallback(
    (dto: { data: AvtaleDto }) => onSuccess(dto.data.id),
    [onSuccess],
  );
  const handleValidationError = useCallback(
    (validation: ValidationError) => {
      validation.errors.forEach((error) => {
        const name = mapNameToSchemaPropertyName(jsonPointerToFieldPath(error.pointer));
        form.setError(name, { type: "custom", message: error.detail });
      });

      function mapNameToSchemaPropertyName(name: string) {
        const mapping: { [name: string]: string } = {
          startDato: "startOgSluttDato.startDato",
          sluttDato: "startOgSluttDato.sluttDato",
          opsjonsmodell: "opsjonsmodell.type",
          opsjonMaksVarighet: "opsjonsmodellD.opsjonMaksVarighet",
          customOpsjonsmodellNavn: "opsjonsmodell.customOpsjonsmodellNavn",
          tiltakstypeId: "tiltakstype",
          utdanningslop: "utdanningslop.utdanninger",
        };
        return (mapping[name] ?? name) as keyof InferredAvtaleSchema;
      }
    },
    [form],
  );

  const hasDetaljerErrors = Object.keys(errors).some(
    (e) => e !== "faneinnhold" && e !== "personvernBekreftet",
  );

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
              {enableTilsagn && (
                <TabWithErrorBorder
                  onClick={() => setActiveTab("okonomi")}
                  value="okonomi"
                  label="Økonomi"
                  hasError={Boolean(errors.prismodell)}
                />
              )}
              <TabWithErrorBorder
                onClick={() => setActiveTab("personvern")}
                value="personvern"
                label="Personvern"
                hasError={Boolean(errors.personvernBekreftet)}
              />
              <TabWithErrorBorder
                label="Redaksjonelt innhold"
                value="redaksjonelt-innhold"
                onClick={() => setActiveTab("redaksjonelt-innhold")}
                hasError={Boolean(errors.faneinnhold)}
              />
            </div>
            <AvtaleFormKnapperad redigeringsModus={redigeringsModus} onClose={onClose} />
          </Tabs.List>
          <Tabs.Panel value="detaljer">
            <Box marginBlock="4">
              <AvtaleFormDetaljer
                avtale={avtale}
                tiltakstyper={props.tiltakstyper}
                ansatt={ansatt}
                enheter={props.enheter}
                okonomiTabEnabled={enableTilsagn}
              />
            </Box>
          </Tabs.Panel>
          {enableTilsagn && (
            <Tabs.Panel value="okonomi">
              <InlineErrorBoundary>
                <Box marginBlock="4">
                  <AvtalePrisOgFakturering tiltakstype={watchedTiltakstype} />
                </Box>
              </InlineErrorBoundary>
            </Tabs.Panel>
          )}
          <Tabs.Panel value="personvern">
            <InlineErrorBoundary>
              <React.Suspense fallback={<Laster tekst="Laster innhold" />}>
                <Box marginBlock="4">
                  <AvtalePersonvernForm tiltakstype={watchedTiltakstype} />
                </Box>
              </React.Suspense>
            </InlineErrorBoundary>
          </Tabs.Panel>
          <Tabs.Panel value="redaksjonelt-innhold">
            <Box marginBlock="4">
              <AvtaleRedaksjoneltInnholdForm tiltakstype={watchedTiltakstype} />
            </Box>
          </Tabs.Panel>
        </Tabs>
        <Separator />
        <AvtaleFormKnapperad redigeringsModus={redigeringsModus} onClose={onClose} />
      </form>
    </FormProvider>
  );
}

/**
 * Så lenge det mangler validering av utdanningsløp i frontend så trenger vi litt ekstra sanitering av data
 */
function getUtdanningslop(data: InferredAvtaleSchema): UtdanningslopDbo | null {
  if (data.tiltakstype.tiltakskode !== Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING) {
    return null;
  }

  if (!data.utdanningslop?.utdanningsprogram || !data.utdanningslop?.utdanninger) {
    return null;
  }

  return data.utdanningslop;
}
