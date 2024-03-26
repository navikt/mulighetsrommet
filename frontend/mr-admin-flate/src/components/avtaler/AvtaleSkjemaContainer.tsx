import { zodResolver } from "@hookform/resolvers/zod";
import { ExclamationmarkTriangleFillIcon } from "@navikt/aksel-icons";
import { Button, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  Avtale,
  AvtaleRequest,
  Avtalestatus,
  EmbeddedTiltakstype,
  NavAnsatt,
  NavEnhet,
  Tiltakstype,
} from "mulighetsrommet-api-client";
import { useRef } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import { avtaleDetaljerTabAtom } from "@/api/atoms";
import { useUpsertAvtale } from "@/api/avtaler/useUpsertAvtale";
import { useHandleApiUpsertResponse } from "@/api/effects";
import { erAnskaffetTiltak } from "../../utils/tiltakskoder";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";
import { Separator } from "../detaljside/Metadata";
import { AvbrytAvtaleModal } from "../modal/AvbrytAvtaleModal";
import { AvtaleSchema, InferredAvtaleSchema } from "../redaksjonelt-innhold/AvtaleSchema";
import skjemastyles from "../skjema/Skjema.module.scss";
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

  const avbrytModalRef = useRef<HTMLDialogElement>(null);
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
      arrangorOrganisasjonsnummer: data.arrangorOrganisasjonsnummer,
      arrangorUnderenheter: data.arrangorUnderenheter,
      arrangorKontaktpersonId: data.arrangorKontaktpersonId ?? null,
      navn: data.navn,
      sluttDato: data.startOgSluttDato.sluttDato ?? null,
      startDato: data.startOgSluttDato.startDato,
      tiltakstypeId: data.tiltakstype.id,
      url: data.url || null,
      administratorer: data.administratorer,
      avtaletype: data.avtaletype,
      prisbetingelser: erAnskaffetTiltak(data.tiltakstype.arenaKode)
        ? data.prisbetingelser || null
        : null,
      beskrivelse: data.beskrivelse,
      faneinnhold: data.faneinnhold,
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

  const hasErrors = Object.keys(errors).length > 0;

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <Tabs defaultValue={activeTab}>
          <Tabs.List className={skjemastyles.tabslist}>
            <div>
              <Tabs.Tab
                onClick={() => setActiveTab("detaljer")}
                style={{
                  border: hasErrors ? "solid 2px #C30000" : "",
                  borderRadius: hasErrors ? "8px" : 0,
                }}
                value="detaljer"
                label={
                  hasErrors ? (
                    <span style={{ display: "flex", alignContent: "baseline", gap: "0.4rem" }}>
                      <ExclamationmarkTriangleFillIcon aria-label="Detaljer" /> Detaljer
                    </span>
                  ) : (
                    "Detaljer"
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
          <Tabs.Panel value="redaksjonelt-innhold">
            <AvtaleRedaksjoneltInnholdForm tiltakstype={watchedTiltakstype} />
          </Tabs.Panel>
        </Tabs>
        <Separator />
        <div className={skjemastyles.flex_container}>
          <HarSkrivetilgang ressurs="Avtale">
            {avtale && avtale.avtalestatus === Avtalestatus.AKTIV && (
              <Button
                size="small"
                variant="danger"
                type="button"
                onClick={() => avbrytModalRef.current?.showModal()}
                className={skjemastyles.avbryt_knapp}
              >
                Avbryt avtale
              </Button>
            )}
          </HarSkrivetilgang>
          <AvtaleSkjemaKnapperad redigeringsModus={redigeringsModus!} onClose={onClose} />
        </div>
      </form>
      {avtale && <AvbrytAvtaleModal modalRef={avbrytModalRef} avtale={avtale} />}
    </FormProvider>
  );
}
