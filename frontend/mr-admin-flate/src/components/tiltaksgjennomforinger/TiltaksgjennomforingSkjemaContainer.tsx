import { zodResolver } from "@hookform/resolvers/zod";
import { ExclamationmarkTriangleFillIcon } from "@navikt/aksel-icons";
import { Alert, Button, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  Avtale,
  NavAnsatt,
  Tiltaksgjennomforing,
  TiltaksgjennomforingRequest,
} from "mulighetsrommet-api-client";
import { useRef } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import { gjennomforingDetaljerTabAtom } from "../../api/atoms";
import { useHandleApiUpsertResponse } from "../../api/effects";
import { useUpsertTiltaksgjennomforing } from "../../api/tiltaksgjennomforing/useUpsertTiltaksgjennomforing";
import { formaterDatoSomYYYYMMDD } from "../../utils/Utils";
import { Separator } from "../detaljside/Metadata";
import { AvbrytTiltaksgjennomforingModal } from "../modal/AvbrytTiltaksgjennomforingModal";
import skjemastyles from "../skjema/Skjema.module.scss";
import {
  InferredTiltaksgjennomforingSchema,
  TiltaksgjennomforingSchema,
} from "./TiltaksgjennomforingSchema";
import { defaultTiltaksgjennomforingData, erArenaOpphav } from "./TiltaksgjennomforingSkjemaConst";
import { TiltaksgjennomforingSkjemaDetaljer } from "./TiltaksgjennomforingSkjemaDetaljer";
import { TiltaksgjennomforingSkjemaKnapperad } from "./TiltaksgjennomforingSkjemaKnapperad";
import { TiltakgjennomforingRedaksjoneltInnholdForm } from "./TiltaksgjennomforingRedaksjoneltInnholdForm";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";

interface Props {
  onClose: () => void;
  onSuccess: (id: string) => void;
  avtale: Avtale;
  ansatt: NavAnsatt;
  tiltaksgjennomforing?: Tiltaksgjennomforing;
}

export const TiltaksgjennomforingSkjemaContainer = ({
  avtale,
  ansatt,
  tiltaksgjennomforing,
  onClose,
  onSuccess,
}: Props) => {
  const redigeringsModus = !!tiltaksgjennomforing;
  const mutation = useUpsertTiltaksgjennomforing();
  const [activeTab, setActiveTab] = useAtom(gjennomforingDetaljerTabAtom);

  const avbrytModalRef = useRef<HTMLDialogElement>(null);

  const form = useForm<InferredTiltaksgjennomforingSchema>({
    resolver: zodResolver(TiltaksgjennomforingSchema),
    defaultValues: defaultTiltaksgjennomforingData(ansatt, avtale, tiltaksgjennomforing),
  });

  const {
    handleSubmit,
    formState: { errors },
  } = form;

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
      arrangorOrganisasjonsnummer:
        data.tiltaksArrangorUnderenhetOrganisasjonsnummer ||
        tiltaksgjennomforing?.arrangor?.organisasjonsnummer ||
        "",
      tiltaksnummer: tiltaksgjennomforing?.tiltaksnummer ?? null,
      oppstart: data.oppstart,
      apentForInnsok: data.apentForInnsok,
      stengtFra: data.midlertidigStengt.erMidlertidigStengt
        ? formaterDatoSomYYYYMMDD(data.midlertidigStengt.stengtFra)
        : null,
      stengtTil: data.midlertidigStengt.erMidlertidigStengt
        ? formaterDatoSomYYYYMMDD(data.midlertidigStengt.stengtTil)
        : null,
      kontaktpersoner:
        data.kontaktpersoner
          ?.filter((kontakt) => kontakt.navIdent !== "")
          ?.map((kontakt) => ({
            ...kontakt,
            navEnheter: kontakt.navEnheter,
          })) || [],
      stedForGjennomforing: data.stedForGjennomforing,
      arrangorKontaktpersonId: data.arrangorKontaktpersonId ?? null,
      beskrivelse: data.beskrivelse,
      faneinnhold: data.faneinnhold ?? null,
      opphav: data.opphav,
      fremmoteTidspunkt: fremmoteTidspunkt(data.fremmoteDato, data.fremmoteTid),
      fremmoteSted: data.fremmoteSted || null,
      deltidsprosent: data.deltidsprosent,
    };

    mutation.mutate(body);
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
          arrangorOrganisasjonsnummer: "tiltaksArrangorUnderenhetOrganisasjonsnummer",
          stengtFra: "midlertidigStengt.erMidlertidigStengt",
          stengtTil: "midlertidigStengt.erMidlertidigStengt",
        };
        return (mapping[name] ?? name) as keyof InferredTiltaksgjennomforingSchema;
      }
    },
  );

  const hasErrors = () => Object.keys(errors).length > 0;

  if (hasErrors()) {
    // eslint-disable-next-line no-console
    console.error(errors);
  }

  return (
    <FormProvider {...form}>
      {!redigeringsModus ? (
        <Alert variant="warning" style={{ margin: "1rem 0" }}>
          Opprettelse av gjennomføring her vil ikke opprette gjennomføringen i Arena.
        </Alert>
      ) : null}
      <form onSubmit={handleSubmit(postData)}>
        <Tabs defaultValue={activeTab}>
          <Tabs.List className={skjemastyles.tabslist}>
            <div>
              <Tabs.Tab
                onClick={() => setActiveTab("detaljer")}
                style={{
                  border: hasErrors() ? "solid 2px #C30000" : "",
                  borderRadius: hasErrors() ? "8px" : 0,
                }}
                value="detaljer"
                label={
                  hasErrors() ? (
                    <span style={{ display: "flex", alignContent: "baseline", gap: "0.4rem" }}>
                      <ExclamationmarkTriangleFillIcon aria-label="Detaljer" /> Detaljer
                    </span>
                  ) : (
                    "Detaljer"
                  )
                }
              />
              <Tabs.Tab
                onClick={() => setActiveTab("redaksjonelt_innhold")}
                value="redaksjonelt_innhold"
                label="Redaksjonelt innhold"
              />
            </div>
            <TiltaksgjennomforingSkjemaKnapperad
              size="small"
              redigeringsModus={redigeringsModus}
              onClose={onClose}
              mutation={mutation}
            />
          </Tabs.List>
          <Tabs.Panel value="detaljer">
            <TiltaksgjennomforingSkjemaDetaljer
              avtale={avtale}
              tiltaksgjennomforing={tiltaksgjennomforing}
            />
          </Tabs.Panel>
          <Tabs.Panel value="redaksjonelt_innhold">
            <TiltakgjennomforingRedaksjoneltInnholdForm avtale={avtale} />
          </Tabs.Panel>
        </Tabs>
        <Separator />
        <div className={skjemastyles.flex_container}>
          <HarSkrivetilgang ressurs="Tiltaksgjennomføring">
            {!erArenaOpphav(tiltaksgjennomforing) && redigeringsModus && (
              <Button
                size="small"
                variant="danger"
                type="button"
                onClick={() => avbrytModalRef.current?.showModal()}
              >
                Avbryt gjennomføring
              </Button>
            )}
          </HarSkrivetilgang>
          <TiltaksgjennomforingSkjemaKnapperad
            size="small"
            redigeringsModus={redigeringsModus}
            onClose={onClose}
            mutation={mutation}
          />
        </div>
      </form>
      {tiltaksgjennomforing && (
        <AvbrytTiltaksgjennomforingModal
          modalRef={avbrytModalRef}
          tiltaksgjennomforing={tiltaksgjennomforing}
        />
      )}
    </FormProvider>
  );
};

function fremmoteTidspunkt(fremmoteDato?: string, fremmoteTid?: string): string | null {
  if (!fremmoteDato) return null;

  return fremmoteTid ? `${fremmoteDato}T${fremmoteTid}:00.000` : `${fremmoteDato}T00:00:00.000`;
}
