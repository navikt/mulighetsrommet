import { zodResolver } from "@hookform/resolvers/zod";
import { ExclamationmarkTriangleFillIcon } from "@navikt/aksel-icons";
import { Button, Tabs } from "@navikt/ds-react";
import {
  Avtale,
  AvtaleRequest,
  Avtalestatus,
  Avtaletype,
  EmbeddedTiltakstype,
  NavAnsatt,
  NavEnhet,
  Opphav,
  Tiltakskode,
  Tiltakstype,
} from "mulighetsrommet-api-client";
import { useEffect, useRef, useState } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import { useHandleApiUpsertResponse } from "../../api/effects";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";
import { formaterDatoTid } from "../../utils/Utils";
import { Separator } from "../detaljside/Metadata";
import skjemastyles from "../skjema/Skjema.module.scss";
import { useUpsertAvtale } from "../../api/avtaler/useUpsertAvtale";
import { AvtaleSchema, InferredAvtaleSchema } from "./AvtaleSchema";
import { erAnskaffetTiltak } from "../../utils/tiltakskoder";
import { AvtaleUtkastData, saveUtkast, utkastDataEllerDefault } from "./AvtaleSkjemaConst";
import { useAtom } from "jotai";
import { avtaleDetaljerTabAtom } from "../../api/atoms";
import { AvtaleSkjemaKnapperad } from "./AvtaleSkjemaKnapperad";
import { AvbrytAvtaleModal } from "../modal/AvbrytAvtaleModal";
import { AvtaleSkjemaDetaljer } from "./AvtaleSkjemaDetaljer";
import { RedaksjoneltInnholdSkjema } from "../tiltaksgjennomforinger/RedaksjoneltInnholdSkjema";

interface Props {
  onClose: () => void;
  onSuccess: (id: string) => void;
  tiltakstyper: Tiltakstype[];
  ansatt: NavAnsatt;
  avtale?: Avtale;
  avtaleUtkast?: AvtaleUtkastData;
  enheter: NavEnhet[];
  redigeringsModus: boolean;
}

export function AvtaleSkjemaContainer({
  onClose,
  onSuccess,
  ansatt,
  avtale,
  avtaleUtkast,
  redigeringsModus,
  ...props
}: Props) {
  const [activeTab, setActiveTab] = useAtom(avtaleDetaljerTabAtom);

  const avbrytModalRef = useRef<HTMLDialogElement>(null);
  const mutation = useUpsertAvtale();
  const mutationUtkast = useMutateUtkast();

  const utkastIdRef = useRef(avtaleUtkast?.id || avtale?.id || uuidv4());

  const form = useForm<InferredAvtaleSchema>({
    resolver: zodResolver(AvtaleSchema),
    defaultValues: utkastDataEllerDefault(ansatt, avtaleUtkast, avtale),
  });

  const {
    handleSubmit,
    formState: { errors, defaultValues },
    watch,
    setValue,
  } = form;

  const watchedTiltakstype: EmbeddedTiltakstype | undefined = watch("tiltakstype");
  const arenaKode = watchedTiltakstype?.arenaKode;

  useEffect(() => {
    // TODO: revurdere behovet for denne type logikk eller om det kan defineres som default felter på tiltakstype i stedet
    // Er det slik at tiltakstype alltid styrer avtaletypen? Er det kun for forhåndsgodkjente avtaler?
    // Hvis ARBFORB og VASV uansett alltid skal være av typen FORHAANDSGODKJENT burde det ikke være mulig å endre
    if (arenaKode === Tiltakskode.ARBFORB || arenaKode === Tiltakskode.VASV) {
      setValue("avtaletype", Avtaletype.FORHAANDSGODKJENT);
    }
  }, [arenaKode]);

  const arenaOpphav = avtale?.opphav === Opphav.ARENA;

  const defaultUpdatedAt = avtale?.updatedAt;
  const [lagreState, setLagreState] = useState(
    defaultUpdatedAt ? `Sist lagret: ${formaterDatoTid(defaultUpdatedAt)}` : undefined,
  );

  const postData: SubmitHandler<InferredAvtaleSchema> = async (data): Promise<void> => {
    const requestBody: AvtaleRequest = {
      id: avtale?.id ?? utkastIdRef.current,
      navEnheter: data.navEnheter.concat(data.navRegioner),
      avtalenummer: avtale?.avtalenummer || null,
      leverandorOrganisasjonsnummer: data.leverandor,
      leverandorUnderenheter: data.leverandorUnderenheter,
      navn: data.navn,
      sluttDato: data.startOgSluttDato.sluttDato,
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
      opphav: data.opphav,
      leverandorKontaktpersonId: data.leverandorKontaktpersonId ?? null,
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
          leverandorOrganisasjonsnummer: "leverandor",
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
            <AvtaleSkjemaKnapperad
              redigeringsModus={redigeringsModus!}
              onClose={onClose}
              defaultValues={defaultValues}
              utkastIdRef={utkastIdRef.current}
              saveUtkast={() =>
                saveUtkast(watch(), avtale!, ansatt, utkastIdRef, mutationUtkast, setLagreState)
              }
              mutationUtkast={mutationUtkast}
              lagreState={lagreState}
              setLagreState={setLagreState}
            />
          </Tabs.List>
          <Tabs.Panel value="detaljer">
            <AvtaleSkjemaDetaljer
              tiltakstyper={props.tiltakstyper}
              ansatt={ansatt}
              enheter={props.enheter}
            />
          </Tabs.Panel>
          <Tabs.Panel value="redaksjonelt-innhold">
            <RedaksjoneltInnholdSkjema tiltakstype={watchedTiltakstype} />
          </Tabs.Panel>
        </Tabs>
        <Separator />
        <div>
          {avtale && !arenaOpphav && avtale.avtalestatus === Avtalestatus.AKTIV && (
            <Button
              size="small"
              variant="danger"
              type="button"
              onClick={() => avbrytModalRef.current?.showModal()}
            >
              Avbryt avtale
            </Button>
          )}
        </div>
      </form>
      {avtale && <AvbrytAvtaleModal modalRef={avbrytModalRef} avtale={avtale} />}
    </FormProvider>
  );
}
