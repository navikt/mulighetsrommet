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
} from "mulighetsrommet-api-client";
import { InlineErrorBoundary } from "mulighetsrommet-frontend-common";
import React from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import { Separator } from "../detaljside/Metadata";
import { Laster } from "../laster/Laster";
import { AvtaleSchema, InferredAvtaleSchema } from "../redaksjonelt-innhold/AvtaleSchema";
import skjemastyles from "../skjema/Skjema.module.scss";
import { AvtalePersonvernForm } from "./AvtalePersonvernForm";
import { AvtaleRedaksjoneltInnholdForm } from "./AvtaleRedaksjoneltInnholdForm";
import { defaultAvtaleData } from "./AvtaleSkjemaConst";
import { AvtaleSkjemaDetaljer } from "./AvtaleSkjemaDetaljer";
import { AvtaleSkjemaKnapperad } from "./AvtaleSkjemaKnapperad";

interface Props {
  onClose: () => void;
  onSuccess: (id: string) => void;
  tiltakstyper: Tiltakstype[];
  ansatt: NavAnsatt;
  avtale?: Avtale;
  enheter: NavEnhet[];
  redigeringsModus: boolean;
}

export function AvtaleSkjemaContainer({
  onClose,
  onSuccess,
  ansatt,
  avtale,
  redigeringsModus,
  ...props
}: Props) {
  const [activeTab, setActiveTab] = useAtom(avtaleDetaljerTabAtom);

  const mutation = useUpsertAvtale();

  const form = useForm<InferredAvtaleSchema>({
    resolver: zodResolver(AvtaleSchema),
    defaultValues: defaultAvtaleData(ansatt, avtale),
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
      prisbetingelser: erAnskaffetTiltak(data.tiltakstype.arenaKode)
        ? data.prisbetingelser || null
        : null,
      beskrivelse: data.beskrivelse,
      faneinnhold: data.faneinnhold,
      personopplysninger: data.personvernBekreftet ? data.personopplysninger : [],
      personvernBekreftet: data.personvernBekreftet,
      amoKategorisering: data.amoKategorisering || null,
      opsjonsmodellData: {
        opsjonMaksVarighet: data.opsjonMaksVarighet || null,
        opsjonsmodell: data.opsjonsmodell || null,
        customOpsjonsmodellNavn: data.customOpsjonsmodellNavn || null,
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
          <Tabs.List className={skjemastyles.tabslist}>
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
            <AvtaleSkjemaKnapperad redigeringsModus={redigeringsModus!} onClose={onClose} />
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
        <div className={skjemastyles.flex_container}>
          <AvtaleSkjemaKnapperad redigeringsModus={redigeringsModus!} onClose={onClose} />
        </div>
      </form>
    </FormProvider>
  );
}
