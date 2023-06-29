import { zodResolver } from "@hookform/resolvers/zod";
import { PlusIcon, XMarkIcon } from "@navikt/aksel-icons";
import { Alert, Button, Checkbox, TextField } from "@navikt/ds-react";
import classNames from "classnames";
import {
  Avtale,
  NavEnhet,
  Tiltaksgjennomforing,
  TiltaksgjennomforingKontaktpersoner,
  TiltaksgjennomforingOppstartstype,
  TiltaksgjennomforingRequest,
} from "mulighetsrommet-api-client";
import { Opphav } from "mulighetsrommet-api-client/build/models/Opphav";
import { Tilgjengelighetsstatus } from "mulighetsrommet-api-client/build/models/Tilgjengelighetsstatus";
import { porten } from "mulighetsrommet-frontend-common/constants";
import React, { Dispatch, SetStateAction, useEffect } from "react";
import {
  FormProvider,
  SubmitHandler,
  useFieldArray,
  useForm,
} from "react-hook-form";
import { Link } from "react-router-dom";
import { v4 as uuidv4 } from "uuid";
import z from "zod";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useHentKontaktpersoner } from "../../api/ansatt/useHentKontaktpersoner";
import { usePutGjennomforing } from "../../api/avtaler/usePutGjennomforing";
import { useAlleEnheter } from "../../api/enhet/useAlleEnheter";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { useVirksomhet } from "../../api/virksomhet/useVirksomhet";
import {
  capitalize,
  formaterDatoSomYYYYMMDD,
  tilgjengelighetsstatusTilTekst,
} from "../../utils/Utils";
import { isTiltakMedFellesOppstart } from "../../utils/tiltakskoder";
import { FormGroup } from "../avtaler/OpprettAvtaleContainer";
import { Laster } from "../laster/Laster";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FraTilDatoVelger } from "../skjema/FraTilDatoVelger";
import { SokeSelect } from "../skjema/SokeSelect";
import styles from "./OpprettTiltaksgjennomforingContainer.module.scss";
import { mulighetsrommetClient } from "../../api/clients";
import { VirksomhetKontaktpersoner } from "../virksomhet/VirksomhetKontaktpersoner";

const Schema = z
  .object({
    tittel: z.string().min(1, "Du må skrive inn tittel"),
    startOgSluttDato: z
      .object({
        startDato: z.date({
          required_error: "En gjennomføring må ha en startdato",
        }),
        sluttDato: z.date({
          required_error: "En gjennomføring må ha en sluttdato",
        }),
      })
      .refine(
        (data) =>
          !data.startDato || !data.sluttDato || data.sluttDato > data.startDato,
        {
          message: "Startdato må være før sluttdato",
          path: ["startDato"],
        }
      ),
    antallPlasser: z
      .number({
        invalid_type_error:
          "Du må skrive inn antall plasser for gjennomføringen som et positivt heltall",
      })
      .int()
      .positive(),
    navEnheter: z
      .string()
      .array()
      .nonempty({ message: "Du må velge minst én enhet" }),
    kontaktpersoner: z
      .object({
        navIdent: z.string({ required_error: "Du må velge en kontaktperson" }),
        navEnheter: z
          .string({ required_error: "Du må velge minst et område" })
          .array(),
      })
      .array()
      .optional(),
    tiltaksArrangorUnderenhetOrganisasjonsnummer: z
      .string({
        required_error: "Du må velge en underenhet for tiltaksarrangør",
      })
      .min(1, "Du må velge en underenhet for tiltaksarrangør"),
    lokasjonArrangor: z.string().refine((data) => data.length > 0, {
      message: "Du må skrive inn lokasjon for hvor gjennomføringen finner sted",
    }),
    arrangorKontaktpersonId: z.string().nullable().optional(),
    ansvarlig: z.string({ required_error: "Du må velge en ansvarlig" }),
    midlertidigStengt: z
      .object({
        erMidlertidigStengt: z.boolean(),
        stengtFra: z.date().optional(),
        stengtTil: z.date().optional(),
      })
      .refine((data) => !data.erMidlertidigStengt || Boolean(data.stengtFra), {
        message: "Midlertidig stengt må ha en start dato",
        path: ["stengtFra"],
      })
      .refine((data) => !data.erMidlertidigStengt || Boolean(data.stengtTil), {
        message: "Midlertidig stengt må ha en til dato",
        path: ["stengtTil"],
      })
      .refine(
        (data) =>
          !data.erMidlertidigStengt ||
          !data.stengtTil ||
          !data.stengtFra ||
          data.stengtTil > data.stengtFra,
        {
          message: "Midlertidig stengt fra dato må være før til dato",
          path: ["stengtFra"],
        }
      ),
    oppstart: z.custom<TiltaksgjennomforingOppstartstype>(
      (val) => !!val,
      "Du må velge oppstartstype"
    ),
    apenForInnsok: z.boolean(),
    estimertVentetid: z.string().optional(),
  })
  .refine(
    (data) =>
      !data.midlertidigStengt.erMidlertidigStengt ||
      !data.midlertidigStengt.stengtTil ||
      !data.startOgSluttDato.sluttDato ||
      data.midlertidigStengt.stengtTil <= data.startOgSluttDato.sluttDato,
    {
      message: "Stengt til dato må være før sluttdato",
      path: ["midlertidigStengt.stengtTil"],
    }
  );

export type inferredSchema = z.infer<typeof Schema>;

interface OpprettTiltaksgjennomforingContainerProps {
  onClose: () => void;
  onSuccess: (id: string) => void;
  setError: Dispatch<SetStateAction<React.ReactNode | null>>;
  avtale?: Avtale;
  tiltaksgjennomforing?: Tiltaksgjennomforing;
}

const tekniskFeilError = () => (
  <>
    Gjennomføringen kunne ikke opprettes på grunn av en teknisk feil hos oss.
    Forsøk på nytt eller ta <a href={porten}>kontakt</a> i Porten dersom du
    trenger mer hjelp.
  </>
);

const avtaleManglerNavRegionError = (avtaleId?: string) => (
  <>
    Avtalen mangler NAV region. Du må oppdatere avtalens NAV region for å kunne
    opprette en gjennomføring.
    {avtaleId ? (
      <>
        <br />
        <br />
        <Link reloadDocument to={`/avtaler/${avtaleId}`}>
          Klikk her for å fikse avtalen
        </Link>
        <br />
        <br />
      </>
    ) : null}
    Ta <a href={porten}>kontakt</a> i Porten dersom du trenger mer hjelp.
  </>
);

const avtaleFinnesIkke = () => (
  <>
    Det finnes ingen avtale koblet til tiltaksgjennomføringen. Hvis
    gjennomføringen er en AFT- eller VTA-gjennomføring kan du koble
    gjennomføringen til riktig avtale.
    <br />
    <br />
    <Link to={`/avtaler`}>Gå til avtaler her</Link>
    <br />
    <br />
    Ta <a href={porten}>kontakt</a> i Porten dersom du trenger mer hjelp.
  </>
);

const avtalenErAvsluttet = () => (
  <>
    Kan ikke opprette gjennomføring fordi avtalens sluttdato har passert.
    <br />
    <br />
    Ta <a href={porten}>kontakt</a> i Porten dersom du trenger mer hjelp.
  </>
);

function defaultOppstartType(
  avtale?: Avtale
): TiltaksgjennomforingOppstartstype {
  if (!avtale) {
    return TiltaksgjennomforingOppstartstype.LOPENDE;
  }

  const tiltakskode = avtale.tiltakstype.arenaKode;
  return isTiltakMedFellesOppstart(tiltakskode)
    ? TiltaksgjennomforingOppstartstype.FELLES
    : TiltaksgjennomforingOppstartstype.LOPENDE;
}

function defaultValuesForKontaktpersoner(
  kontaktpersoner?: TiltaksgjennomforingKontaktpersoner[]
): TiltaksgjennomforingKontaktpersoner[] {
  if (!kontaktpersoner) return [{ navIdent: "", navEnheter: [] }];

  return kontaktpersoner.map((person) => ({
    navIdent: person.navIdent,
    navEnheter:
      person.navEnheter?.length === 0 ? ["alle_enheter"] : person.navEnheter,
  }));
}

export const OpprettTiltaksgjennomforingContainer = (
  props: OpprettTiltaksgjennomforingContainerProps
) => {
  const { data: kontaktpersoner, isLoading: isLoadingKontaktpersoner } =
    useHentKontaktpersoner();
  const mutation = usePutGjennomforing();
  const { avtale, tiltaksgjennomforing, setError, onClose, onSuccess } = props;
  const form = useForm<inferredSchema>({
    resolver: zodResolver(Schema),
    defaultValues: {
      tittel: tiltaksgjennomforing?.navn,
      navEnheter:
        tiltaksgjennomforing?.navEnheter.length === 0
          ? ["alle_enheter"]
          : tiltaksgjennomforing?.navEnheter.map((enhet) => enhet.enhetsnummer),
      ansvarlig: tiltaksgjennomforing?.ansvarlig,
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
        tiltaksgjennomforing?.arrangorOrganisasjonsnummer || "",
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
      apenForInnsok:
        tiltaksgjennomforing?.tilgjengelighet !== Tilgjengelighetsstatus.STENGT,
      kontaktpersoner: defaultValuesForKontaktpersoner(
        tiltaksgjennomforing?.kontaktpersoner
      ),
      estimertVentetid: tiltaksgjennomforing?.estimertVentetid,
      lokasjonArrangor: tiltaksgjennomforing?.lokasjonArrangor,
      arrangorKontaktpersonId: tiltaksgjennomforing?.arrangorKontaktperson?.id,
    },
  });
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
    setValue,
    watch,
  } = form;

  const {
    fields: kontaktpersonFields,
    append: appendKontaktperson,
    remove: removeKontaktperson,
  } = useFieldArray({ name: "kontaktpersoner", control });

  const watchErMidlertidigStengt = watch(
    "midlertidigStengt.erMidlertidigStengt"
  );

  const { data: features } = useFeatureToggles();
  const {
    data: enheter,
    isLoading: isLoadingEnheter,
    isError: isErrorEnheter,
  } = useAlleEnheter();

  const {
    data: ansatt,
    isLoading: isLoadingAnsatt,
    isError: isErrorAnsatt,
  } = useHentAnsatt();

  const { data: virksomhet } = useVirksomhet(
    avtale?.leverandor.organisasjonsnummer || ""
  );

  useEffect(() => {
    if (ansatt && !isLoadingAnsatt && !tiltaksgjennomforing?.ansvarlig) {
      setValue("ansvarlig", ansatt.ident!!);
    }
  }, [ansatt, isLoadingAnsatt, setValue]);

  useEffect(() => {
    if (mutation.data?.id) {
      onSuccess(mutation.data.id);
    }
  }, [mutation]);

  async function getLokasjonForArrangor(arrangorOrgnr?: string) {
    if (!arrangorOrgnr) return;

    const { postnummer = "", poststed = "" } =
      await mulighetsrommetClient.virksomhet.hentVirksomhet({
        orgnr: arrangorOrgnr,
      });

    const lokasjonsStreng = `${postnummer} ${poststed}`.trim();

    if (lokasjonsStreng !== watch("lokasjonArrangor")) {
      setValue("lokasjonArrangor", lokasjonsStreng);
    }
  }

  const redigeringsModus = !!tiltaksgjennomforing;

  const postData: SubmitHandler<inferredSchema> = async (
    data
  ): Promise<void> => {
    if (!features?.["mulighetsrommet.admin-flate-lagre-data-fra-admin-flate"]) {
      alert(
        "Opprettelse av tiltaksgjennomføring er ikke skrudd på enda. Kontakt Team Valp ved spørsmål."
      );
      return;
    }

    const body: TiltaksgjennomforingRequest = {
      id: tiltaksgjennomforing ? tiltaksgjennomforing.id : uuidv4(),
      antallPlasser: data.antallPlasser,
      tiltakstypeId: avtale?.tiltakstype.id || "",
      navEnheter: data.navEnheter.includes("alle_enheter")
        ? []
        : data.navEnheter,
      navn: data.tittel,
      sluttDato: formaterDatoSomYYYYMMDD(data.startOgSluttDato.sluttDato),
      startDato: formaterDatoSomYYYYMMDD(data.startOgSluttDato.startDato),
      avtaleId: avtale?.id || "",
      ansvarlig: data.ansvarlig,
      arrangorOrganisasjonsnummer:
        data.tiltaksArrangorUnderenhetOrganisasjonsnummer ||
        tiltaksgjennomforing?.arrangorOrganisasjonsnummer ||
        "",
      tiltaksnummer: tiltaksgjennomforing?.tiltaksnummer,
      oppstart: data.oppstart,
      apenForInnsok: data.apenForInnsok,
      stengtFra: data.midlertidigStengt.erMidlertidigStengt
        ? formaterDatoSomYYYYMMDD(data.midlertidigStengt.stengtFra)
        : undefined,
      stengtTil: data.midlertidigStengt.erMidlertidigStengt
        ? formaterDatoSomYYYYMMDD(data.midlertidigStengt.stengtTil)
        : undefined,
      kontaktpersoner:
        data.kontaktpersoner
          ?.filter((kontakt) => kontakt.navIdent !== "")
          ?.map((kontakt) => ({
            ...kontakt,
            navEnheter: kontakt.navEnheter.includes("alle_enheter")
              ? []
              : kontakt.navEnheter,
          })) || [],
      estimertVentetid: data.estimertVentetid,
      lokasjonArrangor: data.lokasjonArrangor,
      arrangorKontaktpersonId: data.arrangorKontaktpersonId ?? undefined,
    };

    try {
      await mutation.mutate(body);
    } catch {
      setError(tekniskFeilError());
    }
  };

  const arenaOpphav = tiltaksgjennomforing?.opphav === Opphav.ARENA;

  const navn = ansatt?.fornavn
    ? [ansatt.fornavn, ansatt.etternavn ?? ""]
        .map((it) => capitalize(it))
        .join(" ")
    : "";

  if (!enheter) {
    return <Laster />;
  }

  if (!avtale) {
    setError(avtaleFinnesIkke());
  }

  if (avtale && avtale?.sluttDato && new Date(avtale.sluttDato) < new Date()) {
    setError(avtalenErAvsluttet());
  }

  if (avtale && !avtale?.navRegion) {
    setError(avtaleManglerNavRegionError(avtale?.id));
  }

  if (isErrorAnsatt || isErrorEnheter) {
    setError(tekniskFeilError());
  }

  const enheterOptions = () => {
    const options = enheter
      .filter(
        (enhet: NavEnhet) =>
          avtale?.navRegion?.enhetsnummer === enhet.overordnetEnhet
      )
      .filter(
        (enhet: NavEnhet) =>
          avtale?.navEnheter.length === 0 ||
          avtale?.navEnheter.find((e) => e.enhetsnummer === enhet.enhetsnummer)
      )
      .map((enhet) => ({
        label: enhet.navn,
        value: enhet.enhetsnummer,
      }));

    options?.unshift({ value: "alle_enheter", label: "Alle enheter" });
    return options || [];
  };

  const kontaktpersonerOption = () => {
    const options = kontaktpersoner?.map((kontaktperson) => ({
      label: `${kontaktperson.fornavn} ${kontaktperson.etternavn} - ${kontaktperson.navident}`,
      value: kontaktperson.navident,
    }));

    return options || [];
  };

  const arrangorUnderenheterOptions = () => {
    const options =
      avtale?.leverandorUnderenheter.map((lev) => {
        return {
          label: `${lev.navn} - ${lev.organisasjonsnummer}`,
          value: lev.organisasjonsnummer,
        };
      }) || [];

    // Ingen underenheter betyr at alle er valgt, må gi valg om alle underenheter fra virksomhet
    if (options.length === 0) {
      const enheter = virksomhet?.underenheter || [];
      return enheter.map((enhet) => ({
        value: enhet.organisasjonsnummer,
        label: `${enhet?.navn} - ${enhet?.organisasjonsnummer}`,
      }));
    }
    return options;
  };

  const ansvarligOptions = () => {
    if (isLoadingAnsatt) {
      return [{ label: "Laster...", value: "" }];
    }
    const options = [];
    if (
      tiltaksgjennomforing?.ansvarlig &&
      tiltaksgjennomforing.ansvarlig !== ansatt?.ident
    ) {
      options.push({
        value: tiltaksgjennomforing?.ansvarlig,
        label: tiltaksgjennomforing?.ansvarlig,
      });
    }

    options.push({
      value: ansatt?.ident ?? "",
      label: `${navn} - ${ansatt?.ident}`,
    });

    return options;
  };

  return (
    <FormProvider {...form}>
      {!redigeringsModus ? (
        <Alert variant="warning" style={{ margin: "1rem 0" }}>
          Opprettelse av gjennomføring her vil ikke opprette gjennomføringen i
          Arena.
        </Alert>
      ) : null}
      <form onSubmit={handleSubmit(postData)}>
        <FormGroup>
          <TextField
            size="small"
            readOnly={arenaOpphav}
            error={errors.tittel?.message}
            label="Tiltaksnavn"
            autoFocus
            {...register("tittel")}
          />
        </FormGroup>
        <FormGroup>
          <TextField
            size="small"
            readOnly
            label={"Avtale"}
            value={avtale?.navn || ""}
          />
        </FormGroup>
        <FormGroup>
          <SokeSelect
            size="small"
            label="Oppstartstype"
            readOnly={arenaOpphav}
            onClearValue={() =>
              setValue("oppstart", TiltaksgjennomforingOppstartstype.FELLES)
            }
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
              readOnly: arenaOpphav,
              ...register("startOgSluttDato.startDato"),
            }}
            til={{
              label: "Sluttdato",
              readOnly: arenaOpphav,
              ...register("startOgSluttDato.sluttDato"),
            }}
          />
          <Checkbox
            size="small"
            readOnly={arenaOpphav}
            {...register("apenForInnsok")}
          >
            Åpen for innsøk
          </Checkbox>
          <Checkbox
            size="small"
            {...register("midlertidigStengt.erMidlertidigStengt")}
          >
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
            readOnly={arenaOpphav}
            error={errors.antallPlasser?.message}
            type="number"
            style={{ width: "180px" }}
            label="Antall plasser"
            {...register("antallPlasser", { valueAsNumber: true })}
          />
        </FormGroup>
        <FormGroup>
          <TextField
            readOnly
            size="small"
            label="Tilgjengelighetsstatus"
            description="Statusen vises til veileder i Modia"
            value={tilgjengelighetsstatusTilTekst(
              tiltaksgjennomforing?.tilgjengelighet
            )}
          />
          <TextField
            size="small"
            label="Estimert ventetid"
            description="Kommuniser estimert ventetid til veileder i Modia"
            maxLength={60}
            {...register("estimertVentetid")}
          />
        </FormGroup>
        <FormGroup>
          <TextField
            size="small"
            readOnly
            label={"NAV region"}
            value={avtale?.navRegion?.navn || ""}
          />
          <ControlledMultiSelect
            size="small"
            placeholder={isLoadingEnheter ? "Laster enheter..." : "Velg en"}
            label={"NAV enhet (kontorer)"}
            {...register("navEnheter")}
            options={enheterOptions()}
          />
        </FormGroup>
        <FormGroup>
          <TextField
            size="small"
            label="Tiltaksarrangør hovedenhet"
            placeholder=""
            defaultValue={`${avtale?.leverandor.navn} - ${avtale?.leverandor.organisasjonsnummer}`}
            readOnly
          />
          <div className={styles.virksomhet_kontaktperson_container}>
            <SokeSelect
              size="small"
              label="Tiltaksarrangør underenhet"
              placeholder="Velg underenhet for tiltaksarrangør"
              {...register("tiltaksArrangorUnderenhetOrganisasjonsnummer")}
              onChange={getLokasjonForArrangor}
              onClearValue={() =>
                setValue("tiltaksArrangorUnderenhetOrganisasjonsnummer", "")
              }
              readOnly={!avtale?.leverandor.organisasjonsnummer}
              options={arrangorUnderenheterOptions()}
            />
            {watch('tiltaksArrangorUnderenhetOrganisasjonsnummer') &&
              <VirksomhetKontaktpersoner
                orgnr={watch('tiltaksArrangorUnderenhetOrganisasjonsnummer')}
                formValueName={'arrangorKontaktpersonId'}
              />
            }
          </div>
          <TextField
            size="small"
            label="Sted for gjennomføring"
            description="Sted for gjennomføring, f.eks. Fredrikstad eller Tromsø. Veileder kan filtrere på verdiene i dette feltet, så ikke skriv fulle adresser."
            {...register("lokasjonArrangor")}
            error={
              errors.lokasjonArrangor ? errors.lokasjonArrangor.message : null
            }
          />
        </FormGroup>
        {features?.[
          "mulighetsrommet.admin-flate-koble-tiltaksansvarlig-til-gjennomforing"
        ] ? (
          <FormGroup>
            {kontaktpersonFields.map((field, index) => {
              return (
                <div className={styles.kontaktperson_container} key={field.id}>
                  <button
                    className={classNames(
                      styles.kontaktperson_button,
                      styles.kontaktperson_fjern_button
                    )}
                    type="button"
                    onClick={() => {
                      if (watch("kontaktpersoner")!.length > 1) {
                        removeKontaktperson(index);
                      } else {
                        setValue("kontaktpersoner", [
                          { navIdent: "", navEnheter: [] },
                        ]);
                      }
                    }}
                  >
                    <XMarkIcon />
                  </button>
                  <div className={styles.kontaktperson_inputs}>
                    <SokeSelect
                      size="small"
                      placeholder={
                        isLoadingKontaktpersoner
                          ? "Laster kontaktpersoner..."
                          : "Velg en"
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
                        isLoadingKontaktpersoner
                          ? "Laster enheter..."
                          : "Velg en"
                      }
                      label={"Område"}
                      {...register(`kontaktpersoner.${index}.navEnheter`, {
                        shouldUnregister: true,
                      })}
                      options={enheterOptions()}
                    />
                  </div>
                </div>
              );
            })}
            <button
              className={styles.kontaktperson_button}
              type="button"
              onClick={() =>
                appendKontaktperson({ navIdent: "", navEnheter: [] })
              }
            >
              <PlusIcon /> Legg til ny kontaktperson
            </button>
          </FormGroup>
        ) : null}
        <FormGroup>
          <SokeSelect
            size="small"
            placeholder={
              isLoadingAnsatt ? "Laster Tiltaksansvarlig..." : "Velg en"
            }
            label={"Tiltaksansvarlig"}
            {...register("ansvarlig")}
            options={ansvarligOptions()}
            onClearValue={() => setValue("ansvarlig", "")}
          />
        </FormGroup>

        <div className={styles.button_row}>
          <Button
            className={styles.button}
            onClick={onClose}
            variant="tertiary"
            type="button"
          >
            Avbryt
          </Button>
          <Button
            className={styles.button}
            type="submit"
            disabled={mutation.isLoading}
          >
            {mutation.isLoading
              ? "Lagrer..."
              : redigeringsModus
              ? "Lagre gjennomføring"
              : "Opprett"}
          </Button>
        </div>
      </form>
    </FormProvider>
  );
};
