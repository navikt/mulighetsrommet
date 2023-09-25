import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Button, Checkbox, TextField } from "@navikt/ds-react";
import {
  Avtale,
  Tiltaksgjennomforing,
  TiltaksgjennomforingOppstartstype,
  TiltaksgjennomforingRequest,
  Utkast,
} from "mulighetsrommet-api-client";
import { Tilgjengelighetsstatus } from "mulighetsrommet-api-client/build/models/Tilgjengelighetsstatus";
import skjemastyles from "../skjema/Skjema.module.scss";
import React, { useEffect, useRef } from "react";
import { FormProvider, SubmitHandler, useFieldArray, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import { formaterDatoSomYYYYMMDD, tilgjengelighetsstatusTilTekst } from "../../utils/Utils";
import { AutoSaveUtkast } from "../autosave/AutoSaveUtkast";
import { tekniskFeilError } from "./TiltaksgjennomforingSkjemaErrors";
import {
  inferredTiltaksgjennomforingSchema,
  TiltaksgjennomforingSchema,
} from "./TiltaksgjennomforingSchema";
import {
  arenaOpphav,
  arrangorUnderenheterOptions,
  defaultOppstartType,
  defaultValuesForKontaktpersoner,
  UtkastData,
} from "./TiltaksgjennomforingSkjemaConst";
import { mulighetsrommetClient } from "../../api/clients";
import { useHentKontaktpersoner } from "../../api/ansatt/useHentKontaktpersoner";
import { usePutGjennomforing } from "../../api/avtaler/usePutGjennomforing";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";
import { useHentBetabrukere } from "../../api/ansatt/useHentBetabrukere";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { toast } from "react-toastify";
import { SokeSelect } from "../skjema/SokeSelect";
import { FormGroup } from "../skjema/FormGroup";
import { AvbrytTiltaksgjennomforing } from "./AvbrytTiltaksgjennomforing";
import { TiltaksgjennomforingSkjemaKnapperad } from "./TiltaksgjennomforingSkjemaKnapperad";
import { useVirksomhet } from "../../api/virksomhet/useVirksomhet";
import { PlusIcon, XMarkIcon } from "@navikt/aksel-icons";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FraTilDatoVelger } from "../skjema/FraTilDatoVelger";
import { VirksomhetKontaktpersoner } from "../virksomhet/VirksomhetKontaktpersoner";
import { Separator } from "../detaljside/Metadata";

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
  const { data: virksomhet } = useVirksomhet(avtale.leverandor.organisasjonsnummer || "");
  const mutation = usePutGjennomforing();
  const mutationUtkast = useMutateUtkast();
  const { data: betabrukere } = useHentBetabrukere();

  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();

  const { data: kontaktpersoner, isLoading: isLoadingKontaktpersoner } = useHentKontaktpersoner();

  const kontaktpersonerOption = () => {
    const options = kontaktpersoner?.map((kontaktperson) => ({
      label: `${kontaktperson.fornavn} ${kontaktperson.etternavn} - ${kontaktperson.navIdent}`,
      value: kontaktperson.navIdent,
    }));

    return options || [];
  };

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
      lokasjonArrangor: values?.lokasjonArrangor,
      estimertVentetid: values?.estimertVentetid,
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
      lokasjonArrangor: tiltaksgjennomforing?.lokasjonArrangor,
      arrangorKontaktpersonId: tiltaksgjennomforing?.arrangor?.kontaktperson?.id,
      faneinnhold: tiltaksgjennomforing?.faneinnhold,
    },
  });

  const {
    register,
    handleSubmit,
    control,
    formState: { errors, defaultValues },
    setValue,
    watch,
  } = form;

  const {
    fields: kontaktpersonFields,
    append: appendKontaktperson,
    remove: removeKontaktperson,
  } = useFieldArray({
    name: "kontaktpersoner",
    control,
  });

  const watchErMidlertidigStengt = watch("midlertidigStengt.erMidlertidigStengt");

  async function getLokasjonForArrangor(orgnr?: string) {
    if (!orgnr) return;

    const { postnummer = "", poststed = "" } =
      await mulighetsrommetClient.virksomhet.hentVirksomhet({
        orgnr,
      });

    const lokasjonsStreng = `${postnummer} ${poststed}`.trim();

    if (lokasjonsStreng !== watch("lokasjonArrangor")) {
      setValue("lokasjonArrangor", lokasjonsStreng);
    }
  }

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
      sluttDato: formaterDatoSomYYYYMMDD(data.startOgSluttDato.sluttDato),
      startDato: formaterDatoSomYYYYMMDD(data.startOgSluttDato.startDato),
      avtaleId: avtale.id,
      administrator: data.administrator,
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
      lokasjonArrangor: data.lokasjonArrangor,
      arrangorKontaktpersonId: data.arrangorKontaktpersonId ?? null,
      opphav: tiltaksgjennomforing?.opphav ?? null,
      faneinnhold: data.faneinnhold ?? null,
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

  const navEnheterOptions = avtale.navEnheter.map((enhet) => ({
    value: enhet.enhetsnummer,
    label: enhet.navn,
  }));

  return (
    <FormProvider {...form}>
      {!redigeringsModus ? (
        <Alert variant="warning" style={{ margin: "1rem 0" }}>
          Opprettelse av gjennomføring her vil ikke opprette gjennomføringen i Arena.
        </Alert>
      ) : null}
      <form onSubmit={handleSubmit(postData)}>
        <div className={skjemastyles.container}>
          <Separator />
          <div className={skjemastyles.input_container}>
            <div className={skjemastyles.column}>
              <FormGroup>
                <TextField
                  size="small"
                  readOnly={arenaOpphav(tiltaksgjennomforing)}
                  error={errors.navn?.message}
                  label="Tiltaksnavn"
                  autoFocus
                  data-testid="tiltaksgjennomforingnavn-input"
                  {...register("navn")}
                />
              </FormGroup>
              <Separator />
              <FormGroup>
                <TextField size="small" readOnly label={"Avtale"} value={avtale.navn || ""} />
              </FormGroup>
              <Separator />
              <FormGroup>
                <SokeSelect
                  size="small"
                  label="Oppstartstype"
                  readOnly={arenaOpphav(tiltaksgjennomforing)}
                  placeholder="Velg oppstart"
                  {...register("oppstart")}
                  options={[
                    {
                      label: "Felles oppstartsdato",
                      value: TiltaksgjennomforingOppstartstype.FELLES,
                    },
                    {
                      label: "Løpende oppstart",
                      value: TiltaksgjennomforingOppstartstype.LOPENDE,
                    },
                  ]}
                />
                <FraTilDatoVelger
                  size="small"
                  fra={{
                    label: "Startdato",
                    readOnly: arenaOpphav(tiltaksgjennomforing),
                    ...register("startOgSluttDato.startDato"),
                  }}
                  til={{
                    label: "Sluttdato",
                    readOnly:
                      arenaOpphav(tiltaksgjennomforing) && !!tiltaksgjennomforing?.sluttDato,
                    ...register("startOgSluttDato.sluttDato"),
                  }}
                />
                <Checkbox
                  size="small"
                  readOnly={arenaOpphav(tiltaksgjennomforing)}
                  {...register("apenForInnsok")}
                >
                  Åpen for innsøk
                </Checkbox>
                <Checkbox size="small" {...register("midlertidigStengt.erMidlertidigStengt")}>
                  Midlertidig stengt
                </Checkbox>
                {watchErMidlertidigStengt && (
                  <FraTilDatoVelger
                    size="small"
                    fra={{
                      label: "Stengt fra",
                      ...register("midlertidigStengt.stengtFra"),
                    }}
                    til={{
                      label: "Stengt til",
                      ...register("midlertidigStengt.stengtTil"),
                    }}
                  />
                )}
                <TextField
                  size="small"
                  readOnly={arenaOpphav(tiltaksgjennomforing)}
                  error={errors.antallPlasser?.message}
                  type="number"
                  style={{
                    width: "180px",
                  }}
                  label="Antall plasser"
                  {...register("antallPlasser", {
                    valueAsNumber: true,
                  })}
                />
                {!arenaOpphav(tiltaksgjennomforing) && redigeringsModus ? (
                  <AvbrytTiltaksgjennomforing onAvbryt={onClose} />
                ) : null}
              </FormGroup>
              <Separator />
              <FormGroup>
                <TextField
                  readOnly
                  size="small"
                  label="Tilgjengelighetsstatus"
                  description="Statusen vises til veileder i Modia"
                  value={tilgjengelighetsstatusTilTekst(tiltaksgjennomforing?.tilgjengelighet)}
                />
                <TextField
                  size="small"
                  label="Estimert ventetid"
                  description="Kommuniser estimert ventetid til veileder i Modia"
                  maxLength={60}
                  {...register("estimertVentetid")}
                />
              </FormGroup>
              <Separator />
              <FormGroup>
                <SokeSelect
                  size="small"
                  placeholder={isLoadingAnsatt ? "Laster..." : "Velg en"}
                  label={"Administrator for gjennomføringen"}
                  {...register("administrator")}
                  options={AdministratorOptions(
                    ansatt,
                    tiltaksgjennomforing?.administrator,
                    betabrukere,
                  )}
                  onClearValue={() => setValue("administrator", "")}
                />
              </FormGroup>
            </div>
            <div className={skjemastyles.vertical_separator} />
            <div className={skjemastyles.column}>
              <div className={skjemastyles.gray_container}>
                <FormGroup>
                  <TextField
                    size="small"
                    readOnly
                    label={"NAV-region"}
                    value={avtale.navRegion?.navn || ""}
                  />
                  <ControlledMultiSelect
                    size="small"
                    placeholder={"Velg en"}
                    label={"NAV-enheter (kontorer)"}
                    {...register("navEnheter")}
                    options={navEnheterOptions}
                  />
                </FormGroup>
                <Separator />
                <FormGroup>
                  <div>
                    {kontaktpersonFields?.map((field, index) => {
                      return (
                        <div className={skjemastyles.kontaktperson_container} key={field.id}>
                          <button
                            className={skjemastyles.kontaktperson_button}
                            type="button"
                            onClick={() => {
                              if (watch("kontaktpersoner")!.length > 1) {
                                removeKontaktperson(index);
                              } else {
                                setValue("kontaktpersoner", [
                                  {
                                    navIdent: "",
                                    navEnheter: [],
                                  },
                                ]);
                              }
                            }}
                          >
                            <XMarkIcon fontSize="1.5rem" />
                          </button>
                          <div className={skjemastyles.kontaktperson_inputs}>
                            <SokeSelect
                              size="small"
                              placeholder={
                                isLoadingKontaktpersoner ? "Laster kontaktpersoner..." : "Velg en"
                              }
                              label={"Kontaktperson i NAV"}
                              {...register(`kontaktpersoner.${index}.navIdent`, {
                                shouldUnregister: true,
                              })}
                              options={kontaktpersonerOption()}
                            />
                            <ControlledMultiSelect
                              size="small"
                              placeholder={
                                isLoadingKontaktpersoner ? "Laster enheter..." : "Velg en"
                              }
                              label={"Område"}
                              {...register(`kontaktpersoner.${index}.navEnheter`, {
                                shouldUnregister: true,
                              })}
                              options={navEnheterOptions}
                            />
                          </div>
                        </div>
                      );
                    })}
                    <Button
                      className={skjemastyles.kontaktperson_button}
                      type="button"
                      size="small"
                      onClick={() =>
                        appendKontaktperson({
                          navIdent: "",
                          navEnheter: [],
                        })
                      }
                    >
                      <PlusIcon /> Legg til ny kontaktperson
                    </Button>
                  </div>
                </FormGroup>
              </div>
              <div className={skjemastyles.gray_container}>
                <FormGroup>
                  <TextField
                    size="small"
                    label="Tiltaksarrangør hovedenhet"
                    placeholder=""
                    defaultValue={`${avtale.leverandor.navn} - ${avtale.leverandor.organisasjonsnummer}`}
                    readOnly
                  />
                  <SokeSelect
                    size="small"
                    label="Tiltaksarrangør underenhet"
                    placeholder="Velg underenhet for tiltaksarrangør"
                    {...register("tiltaksArrangorUnderenhetOrganisasjonsnummer")}
                    onChange={() => {
                      getLokasjonForArrangor();
                      setValue("arrangorKontaktpersonId", null);
                    }}
                    onClearValue={() => {
                      setValue("tiltaksArrangorUnderenhetOrganisasjonsnummer", "");
                    }}
                    readOnly={!avtale.leverandor.organisasjonsnummer}
                    options={arrangorUnderenheterOptions(avtale, virksomhet)}
                  />
                  {watch("tiltaksArrangorUnderenhetOrganisasjonsnummer") &&
                    !tiltaksgjennomforing?.arrangor?.slettet && (
                      <div className={skjemastyles.virksomhet_kontaktperson_container}>
                        <VirksomhetKontaktpersoner
                          title={"Kontaktperson hos arrangøren"}
                          orgnr={watch("tiltaksArrangorUnderenhetOrganisasjonsnummer")}
                          formValueName={"arrangorKontaktpersonId"}
                        />
                      </div>
                    )}
                  <TextField
                    size="small"
                    label="Sted for gjennomføring"
                    description="Skriv inn stedet tiltaket skal gjennomføres, for eksempel Fredrikstad eller Tromsø. For tiltak uten eksplisitt lokasjon (for eksempel digital jobbklubb), kan du la feltet stå tomt."
                    {...register("lokasjonArrangor")}
                    error={errors.lokasjonArrangor ? errors.lokasjonArrangor.message : null}
                  />
                </FormGroup>
              </div>
            </div>
          </div>
          <Separator />
          <TiltaksgjennomforingSkjemaKnapperad
            redigeringsModus={redigeringsModus}
            onClose={onClose}
            mutation={mutation}
          />
        </div>
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
