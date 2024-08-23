import { avtaleDetaljerTabAtom } from "@/api/atoms";
import { useUpsertAvtale } from "@/api/avtaler/useUpsertAvtale";
import { useHandleApiUpsertResponse } from "@/api/effects";
import { erAnskaffetTiltak } from "@/utils/tiltakskoder";
import { zodResolver } from "@hookform/resolvers/zod";
import { ExclamationmarkTriangleFillIcon } from "@navikt/aksel-icons";
import { Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  Avtale,
  AvtaleRequest,
  EmbeddedTiltakstype,
  NavAnsatt,
  NavEnhet,
  Tiltakstype,
} from "@mr/api-client";
import React from "react";
import { DeepPartial, FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import { Separator } from "../detaljside/Metadata";
import { AvtaleSchema, InferredAvtaleSchema } from "@/components/redaksjoneltInnhold/AvtaleSchema";
import { AvtaleRedaksjoneltInnholdForm } from "./AvtaleRedaksjoneltInnholdForm";
import { AvtaleSkjemaDetaljer } from "./AvtaleSkjemaDetaljer";
import { AvtaleSkjemaKnapperad } from "./AvtaleSkjemaKnapperad";
import { AvtalePersonvernForm } from "./AvtalePersonvernForm";
import { Laster } from "../laster/Laster";
import { InlineErrorBoundary } from "@mr/frontend-common";
import { RedaksjoneltInnholdBunnKnapperad } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdBunnKnapperad";
import styles from "./AvtaleSkjemaContainer.module.scss";

interface Props {
  onClose: () => void;
  onSuccess: (id: string) => void;
  tiltakstyper: Tiltakstype[];
  ansatt: NavAnsatt;
  avtale?: Avtale;
  enheter: NavEnhet[];
  redigeringsModus: boolean;
  defaultValues: DeepPartial<InferredAvtaleSchema>;
}

export function AvtaleSkjemaContainer({
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

  const form = useForm<InferredAvtaleSchema>({
    resolver: zodResolver(AvtaleSchema),
    defaultValues,
  });

  const {
    handleSubmit,
    formState: { errors },
    watch,
  } = form;

  const watchedTiltakstype: EmbeddedTiltakstype | undefined = watch("tiltakstype");

  const postData: SubmitHandler<InferredAvtaleSchema> = async (data): Promise<void> => {
    const requestBody: AvtaleRequest = {
      id: avtale?.id ?? uuidv4(),
      navEnheter: data.navEnheter.concat(data.navRegioner),
      avtalenummer: avtale?.avtalenummer || null,
      websaknummer: data.websaknummer || null,
      arrangorOrganisasjonsnummer: data.arrangorOrganisasjonsnummer,
      arrangorUnderenheter: data.arrangorUnderenheter,
      arrangorKontaktpersoner: data.arrangorKontaktpersoner,
      navn: data.navn,
      sluttDato: data.startOgSluttDato.sluttDato || null,
      startDato: data.startOgSluttDato.startDato,
      tiltakstypeId: data.tiltakstype.id,
      administratorer: data.administratorer,
      avtaletype: data.avtaletype,
      prisbetingelser: erAnskaffetTiltak(data.tiltakstype.tiltakskode)
        ? data.prisbetingelser || null
        : null,
      beskrivelse: data.beskrivelse,
      faneinnhold: data.faneinnhold,
      personopplysninger: data.personvernBekreftet ? data.personopplysninger : [],
      personvernBekreftet: data.personvernBekreftet,
      amoKategorisering: data.amoKategorisering || null,
      opsjonsmodellData: {
        opsjonMaksVarighet: data?.opsjonsmodellData?.opsjonMaksVarighet || null,
        opsjonsmodell: data?.opsjonsmodellData?.opsjonsmodell || null,
        customOpsjonsmodellNavn: data?.opsjonsmodellData?.customOpsjonsmodellNavn || null,
      },
    };

    mutation.mutate(requestBody);
  };

  useHandleApiUpsertResponse(
    mutation,
    (response) => onSuccess(response.id),
    (validation) => {
      validation.errors.forEach((error) => {
        const name = mapErrorToSchemaPropertyName(error.name);
        form.setError(name, { type: "custom", message: error.message });
      });

      function mapErrorToSchemaPropertyName(name: string) {
        const mapping: { [name: string]: string } = {
          startDato: "startOgSluttDato.startDato",
          sluttDato: "startOgSluttDato.sluttDato",
          opsjonsmodell: "opsjonsmodellData.opsjonsmodell",
          opsjonMaksVarighet: "opsjonsmodellData.opsjonMaksVarighet",
          customOpsjonsmodellNavn: "opsjonsmodellData.customOpsjonsmodellNavn",
          tiltakstypeId: "tiltakstype",
        };
        return (mapping[name] ?? name) as keyof InferredAvtaleSchema;
      }
    },
  );

  const hasPersonvernErrors = Boolean(errors?.personvernBekreftet);
  const hasDetaljerErrors = Object.keys(errors).length > (hasPersonvernErrors ? 1 : 0);

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <Tabs defaultValue={activeTab}>
          <Tabs.List className={styles.tabslist}>
            <div>
              <Tabs.Tab
                onClick={() => setActiveTab("detaljer")}
                style={{
                  border: hasDetaljerErrors ? "solid 2px #C30000" : "",
                  borderRadius: hasDetaljerErrors ? "8px" : 0,
                }}
                value="detaljer"
                label={
                  hasDetaljerErrors ? (
                    <span style={{ display: "flex", alignContent: "baseline", gap: "0.4rem" }}>
                      <ExclamationmarkTriangleFillIcon aria-label="Detaljer" /> Detaljer
                    </span>
                  ) : (
                    "Detaljer"
                  )
                }
              />
              <Tabs.Tab
                onClick={() => setActiveTab("personvern")}
                style={{
                  border: hasPersonvernErrors ? "solid 2px #C30000" : "",
                  borderRadius: hasPersonvernErrors ? "8px" : 0,
                }}
                value="personvern"
                label={
                  hasPersonvernErrors ? (
                    <span style={{ display: "flex", alignContent: "baseline", gap: "0.4rem" }}>
                      <ExclamationmarkTriangleFillIcon aria-label="Personvern" /> Personvern
                    </span>
                  ) : (
                    "Personvern"
                  )
                }
              />
              <Tabs.Tab
                label="Redaksjonelt innhold"
                value="redaksjonelt-innhold"
                onClick={() => setActiveTab("redaksjonelt-innhold")}
              />
            </div>
            <AvtaleSkjemaKnapperad redigeringsModus={redigeringsModus} onClose={onClose} />
          </Tabs.List>
          <Tabs.Panel value="detaljer">
            <AvtaleSkjemaDetaljer
              avtale={avtale}
              tiltakstyper={props.tiltakstyper}
              ansatt={ansatt}
              enheter={props.enheter}
            />
          </Tabs.Panel>
          <Tabs.Panel value="personvern">
            <InlineErrorBoundary>
              <React.Suspense fallback={<Laster tekst="Laster innhold" />}>
                <AvtalePersonvernForm tiltakstypeId={watchedTiltakstype?.id} />
              </React.Suspense>
            </InlineErrorBoundary>
          </Tabs.Panel>
          <Tabs.Panel value="redaksjonelt-innhold">
            <AvtaleRedaksjoneltInnholdForm tiltakstype={watchedTiltakstype} />
          </Tabs.Panel>
        </Tabs>
        <Separator />
        <RedaksjoneltInnholdBunnKnapperad>
          <AvtaleSkjemaKnapperad redigeringsModus={redigeringsModus} onClose={onClose} />
        </RedaksjoneltInnholdBunnKnapperad>
      </form>
    </FormProvider>
  );
}
