import { zodResolver } from "@hookform/resolvers/zod";
import { ExclamationmarkTriangleFillIcon } from "@navikt/aksel-icons";
import { Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  Avtale,
  NavAnsatt,
  Tiltaksgjennomforing,
  TiltaksgjennomforingRequest,
} from "mulighetsrommet-api-client";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { useHandleApiUpsertResponse } from "@/api/effects";
import { useUpsertTiltaksgjennomforing } from "@/api/tiltaksgjennomforing/useUpsertTiltaksgjennomforing";
import { Separator } from "../detaljside/Metadata";
import skjemastyles from "../skjema/Skjema.module.scss";
import { TiltakgjennomforingRedaksjoneltInnholdForm } from "./TiltaksgjennomforingRedaksjoneltInnholdForm";
import {
  InferredTiltaksgjennomforingSchema,
  TiltaksgjennomforingSchema,
} from "../redaksjonelt-innhold/TiltaksgjennomforingSchema";
import { defaultTiltaksgjennomforingData } from "./TiltaksgjennomforingSkjemaConst";
import { TiltaksgjennomforingSkjemaDetaljer } from "./TiltaksgjennomforingSkjemaDetaljer";
import { TiltaksgjennomforingSkjemaKnapperad } from "./TiltaksgjennomforingSkjemaKnapperad";
import { logEvent } from "../../logging/amplitude";

interface Props {
  onClose: () => void;
  onSuccess: (id: string) => void;
  avtale: Avtale;
  ansatt: NavAnsatt;
  tiltaksgjennomforing?: Tiltaksgjennomforing;
}

function loggRedaktorEndrerTilgjengeligForArrangor(datoValgt: string) {
  logEvent({
    name: "tiltaksadministrasjon.sett-tilgjengelig-for-redaktor",
    data: {
      datoValgt,
    },
  });
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
      arrangorId: data.arrangorId,
      oppstart: data.oppstart,
      apentForInnsok: data.apentForInnsok,
      kontaktpersoner:
        data.kontaktpersoner
          ?.filter((kontakt) => kontakt.navIdent !== null)
          ?.map((kontakt) => ({
            navIdent: kontakt.navIdent!!,
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
      amoKategorisering: avtale.amoKategorisering
        ? {
            kurstype: avtale.amoKategorisering.kurstype,
            spesifisering: avtale.amoKategorisering.spesifisering,
            ...data.amoKategorisering,
          }
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
        };
        return (mapping[name] ?? name) as keyof InferredTiltaksgjennomforingSchema;
      }
    },
  );

  const hasErrors = () => Object.keys(errors).length > 0;

  return (
    <FormProvider {...form}>
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
                onClick={() => setActiveTab("redaksjonelt-innhold")}
                value="redaksjonelt-innhold"
                label="Redaksjonelt innhold"
              />
            </div>
            <TiltaksgjennomforingSkjemaKnapperad
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
          <Tabs.Panel value="redaksjonelt-innhold">
            <TiltakgjennomforingRedaksjoneltInnholdForm avtale={avtale} />
          </Tabs.Panel>
        </Tabs>
        <Separator />
        <div className={skjemastyles.flex_container}>
          <TiltaksgjennomforingSkjemaKnapperad
            redigeringsModus={redigeringsModus}
            onClose={onClose}
            mutation={mutation}
          />
        </div>
      </form>
    </FormProvider>
  );
};
