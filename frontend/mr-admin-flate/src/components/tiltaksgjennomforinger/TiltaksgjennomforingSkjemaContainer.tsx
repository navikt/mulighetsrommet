import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Tabs } from "@navikt/ds-react";
import {
  Avtale,
  Opphav,
  Tiltaksgjennomforing,
  TiltaksgjennomforingRequest,
  Toggles,
  Utkast,
} from "mulighetsrommet-api-client";
import { Tilgjengelighetsstatus } from "mulighetsrommet-api-client/build/models/Tilgjengelighetsstatus";
import skjemastyles from "../skjema/Skjema.module.scss";
import React, { useEffect, useRef } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import { formaterDatoSomYYYYMMDD } from "../../utils/Utils";
import { AutoSaveUtkast } from "../autosave/AutoSaveUtkast";
import { tekniskFeilError } from "./TiltaksgjennomforingSkjemaErrors";
import {
  inferredTiltaksgjennomforingSchema,
  TiltaksgjennomforingSchema,
} from "./TiltaksgjennomforingSchema";
import {
  defaultOppstartType,
  defaultValuesForKontaktpersoner,
  UtkastData,
} from "./TiltaksgjennomforingSkjemaConst";
import { usePutGjennomforing } from "../../api/avtaler/usePutGjennomforing";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { toast } from "react-toastify";
import { TiltaksgjennomforingSkjemaKnapperad } from "./TiltaksgjennomforingSkjemaKnapperad";
import { TiltaksgjennomforingSkjemaDetaljer } from "./TiltaksgjennomforingSkjemaDetaljer";
import { TiltaksgjennomforingSkjemaRedInnhold } from "./TiltaksgjennomforingSkjemaRedInnhold";
import { useFeatureToggle } from "../../api/features/feature-toggles";

interface Props {
  onClose: () => void;
  onSuccess: (id: string) => void;
  avtale: Avtale;
  tiltaksgjennomforing?: Tiltaksgjennomforing;
}

export const TiltaksgjennomforingSkjemaContainer = ({
  avtale,
  tiltaksgjennomforing,
  onClose,
  onSuccess,
}: Props) => {
  const utkastIdRef = useRef(tiltaksgjennomforing?.id || uuidv4());
  const redigeringsModus = !!tiltaksgjennomforing;
  const mutation = usePutGjennomforing();
  const mutationUtkast = useMutateUtkast();
  const { data: visFaneinnhold } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_FANEINNHOLD,
  );

  const { data: ansatt } = useHentAnsatt();

  const saveUtkast = (
    values: inferredTiltaksgjennomforingSchema,
    avtale: Avtale,
    utkastIdRef: React.MutableRefObject<string>,
  ) => {
    if (!avtale) {
      return;
    }

    const utkastData: UtkastData = {
      navn: values?.navn,
      antallPlasser: values?.antallPlasser,
      startDato: values?.startOgSluttDato?.startDato?.toDateString(),
      sluttDato: values?.startOgSluttDato?.sluttDato?.toDateString(),
      navEnheter: values?.navEnheter?.map((enhetsnummer) => ({
        navn: "",
        enhetsnummer,
      })),
      stengtFra: values?.midlertidigStengt?.erMidlertidigStengt
        ? values?.midlertidigStengt?.stengtFra?.toString()
        : undefined,
      stengtTil: values?.midlertidigStengt?.erMidlertidigStengt
        ? values?.midlertidigStengt?.stengtTil?.toString()
        : undefined,
      tiltakstypeId: avtale.tiltakstype.id,
      avtaleId: avtale.id,
      arrangorKontaktpersonId: {
        id: values?.arrangorKontaktpersonId ?? undefined,
      },
      arrangor: {
        organisasjonsnummer: values.tiltaksArrangorUnderenhetOrganisasjonsnummer,
        slettet: false,
      },
      kontaktpersoner: values?.kontaktpersoner?.map((kp) => ({ ...kp })) || [],
      id: utkastIdRef.current,
      stedForGjennomforing: values?.stedForGjennomforing,
      beskrivelse: values?.beskrivelse ?? undefined,
      estimertVentetid: values?.estimertVentetid ?? undefined,
    };

    if (!values.navn) {
      toast.info("For å lagre utkast må du gi utkastet et navn", {
        autoClose: 10000,
      });
      return;
    }

    mutationUtkast.mutate({
      id: utkastIdRef.current,
      utkastData,
      type: Utkast.type.TILTAKSGJENNOMFORING,
      opprettetAv: ansatt?.navIdent,
      avtaleId: avtale.id,
    });
  };

  const form = useForm<inferredTiltaksgjennomforingSchema>({
    resolver: zodResolver(TiltaksgjennomforingSchema),
    defaultValues: {
      navn: tiltaksgjennomforing?.navn,
      navEnheter: tiltaksgjennomforing?.navEnheter?.map((enhet) => enhet.enhetsnummer) || [],
      administrator: tiltaksgjennomforing?.administrator?.navIdent,
      antallPlasser: tiltaksgjennomforing?.antallPlasser,
      startOgSluttDato: {
        startDato: tiltaksgjennomforing?.startDato
          ? new Date(tiltaksgjennomforing.startDato)
          : undefined,
        sluttDato: tiltaksgjennomforing?.sluttDato
          ? new Date(tiltaksgjennomforing.sluttDato)
          : undefined,
      },
      tiltaksArrangorUnderenhetOrganisasjonsnummer:
        tiltaksgjennomforing?.arrangor?.organisasjonsnummer || "",
      midlertidigStengt: {
        erMidlertidigStengt: Boolean(tiltaksgjennomforing?.stengtFra),
        stengtFra: tiltaksgjennomforing?.stengtFra
          ? new Date(tiltaksgjennomforing.stengtFra)
          : undefined,
        stengtTil: tiltaksgjennomforing?.stengtTil
          ? new Date(tiltaksgjennomforing.stengtTil)
          : undefined,
      },
      oppstart: tiltaksgjennomforing?.oppstart ?? defaultOppstartType(avtale),
      apenForInnsok: tiltaksgjennomforing?.tilgjengelighet !== Tilgjengelighetsstatus.STENGT,
      kontaktpersoner: defaultValuesForKontaktpersoner(tiltaksgjennomforing?.kontaktpersoner),
      estimertVentetid: tiltaksgjennomforing?.estimertVentetid,
      stedForGjennomforing: tiltaksgjennomforing?.stedForGjennomforing,
      arrangorKontaktpersonId: tiltaksgjennomforing?.arrangor?.kontaktperson?.id,
      beskrivelse: tiltaksgjennomforing?.beskrivelse ?? null,
      faneinnhold: tiltaksgjennomforing?.faneinnhold ?? {},
      opphav: tiltaksgjennomforing?.opphav ?? Opphav.MR_ADMIN_FLATE,
    },
  });

  const {
    handleSubmit,
    formState: { defaultValues },
    watch,
  } = form;

  const postData: SubmitHandler<inferredTiltaksgjennomforingSchema> = async (
    data,
  ): Promise<void> => {
    if (!avtale) {
      <Alert variant="error">{tekniskFeilError()}</Alert>;
      return;
    }

    const body: TiltaksgjennomforingRequest = {
      id: tiltaksgjennomforing ? tiltaksgjennomforing.id : uuidv4(),
      antallPlasser: data.antallPlasser,
      tiltakstypeId: avtale.tiltakstype.id,
      navEnheter: data.navEnheter,
      navn: data.navn,
      startDato: formaterDatoSomYYYYMMDD(data.startOgSluttDato.startDato),
      sluttDato: data.startOgSluttDato.sluttDato
        ? formaterDatoSomYYYYMMDD(data.startOgSluttDato.sluttDato)
        : null,
      avtaleId: avtale.id,
      administrator: data.administrator!!,
      arrangorOrganisasjonsnummer:
        data.tiltaksArrangorUnderenhetOrganisasjonsnummer ||
        tiltaksgjennomforing?.arrangor?.organisasjonsnummer ||
        "",
      tiltaksnummer: tiltaksgjennomforing?.tiltaksnummer ?? null,
      oppstart: data.oppstart,
      apenForInnsok: data.apenForInnsok,
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
      estimertVentetid: data.estimertVentetid ?? null,
      stedForGjennomforing: data.stedForGjennomforing,
      arrangorKontaktpersonId: data.arrangorKontaktpersonId ?? null,
      beskrivelse: data.beskrivelse,
      faneinnhold: data.faneinnhold,
      opphav: data.opphav,
    };

    try {
      mutation.mutate(body);
    } catch {
      <Alert variant="error">{tekniskFeilError()}</Alert>;
    }
  };

  useEffect(() => {
    if (mutation.isSuccess) {
      onSuccess(mutation.data.id);
    }
  }, [mutation]);

  return (
    <FormProvider {...form}>
      {!redigeringsModus ? (
        <Alert variant="warning" style={{ margin: "1rem 0" }}>
          Opprettelse av gjennomføring her vil ikke opprette gjennomføringen i Arena.
        </Alert>
      ) : null}
      <form onSubmit={handleSubmit(postData)}>
        {visFaneinnhold ? (
          <Tabs defaultValue="detaljer">
            <Tabs.List className={skjemastyles.tabslist}>
              <div>
                <Tabs.Tab value="detaljer" label="Detaljer" />
                <Tabs.Tab value="redaksjonelt_innhold" label="Redaksjonelt innhold" />
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
                onClose={onClose}
              />
            </Tabs.Panel>
            <Tabs.Panel value="redaksjonelt_innhold">
              <TiltaksgjennomforingSkjemaRedInnhold avtale={avtale} />
            </Tabs.Panel>
          </Tabs>
        ) : (
          <>
            <TiltaksgjennomforingSkjemaDetaljer
              avtale={avtale}
              tiltaksgjennomforing={tiltaksgjennomforing}
              onClose={onClose}
            />
            <div className={skjemastyles.button_row}>
              <TiltaksgjennomforingSkjemaKnapperad
                redigeringsModus={redigeringsModus}
                onClose={onClose}
                mutation={mutation}
              />
            </div>
          </>
        )}
      </form>
      <AutoSaveUtkast
        defaultValues={defaultValues}
        utkastId={utkastIdRef.current}
        onSave={() => saveUtkast(watch(), avtale, utkastIdRef)}
        mutation={mutationUtkast}
      />
    </FormProvider>
  );
};
