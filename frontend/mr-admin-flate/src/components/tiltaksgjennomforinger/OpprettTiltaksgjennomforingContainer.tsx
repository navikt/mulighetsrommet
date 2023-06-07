import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Button, Checkbox, TextField } from "@navikt/ds-react";
import {
  Avtale,
  NavEnhet,
  Tiltaksgjennomforing,
  TiltaksgjennomforingOppstartstype,
  TiltaksgjennomforingRequest,
} from "mulighetsrommet-api-client";
import React, { Dispatch, SetStateAction, useEffect } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import z from "zod";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useAlleEnheter } from "../../api/enhet/useAlleEnheter";
import { FraTilDatoVelger } from "../skjema/FraTilDatoVelger";
import { SokeSelect } from "../skjema/SokeSelect";
import styles from "./OpprettTiltaksgjennomforingContainer.module.scss";
import { Laster } from "../laster/Laster";
import { useNavigerTilTiltaksgjennomforing } from "../../hooks/useNavigerTilTiltaksgjennomforing";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FormGroup } from "../avtaler/OpprettAvtaleContainer";
import { porten } from "mulighetsrommet-frontend-common/constants";
import { capitalize, formaterDatoSomYYYYMMDD } from "../../utils/Utils";
import { useVirksomhet } from "../../api/virksomhet/useVirksomhet";
import { Link } from "react-router-dom";
import { isTiltakMedFellesOppstart } from "../../utils/tiltakskoder";
import { mulighetsrommetClient } from "../../api/clients";
import { Opphav } from "mulighetsrommet-api-client/build/models/Opphav";
import { useFeatureToggles } from "../../api/features/feature-toggles";

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
    tiltaksArrangorUnderenhetOrganisasjonsnummer: z
      .string({
        required_error: "Du må velge en underenhet for tiltaksarrangør",
      })
      .min(1, "Du må velge en underenhet for tiltaksarrangør"),
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
  })
  .refine(
    (data) =>
      !data.midlertidigStengt.erMidlertidigStengt ||
      !data.midlertidigStengt.stengtTil ||
      !data.sluttDato ||
      data.midlertidigStengt.stengtTil <= data.sluttDato,
    {
      message: "Stengt til dato må være før sluttdato",
      path: ["midlertidigStengt.stengtTil"],
    }
  );

export type inferredSchema = z.infer<typeof Schema>;

interface OpprettTiltaksgjennomforingContainerProps {
  onAvbryt: () => void;
  setError: Dispatch<SetStateAction<React.ReactNode | null>>;
  setResult: Dispatch<SetStateAction<string | null>>;
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
        <Link to={`/avtaler/${avtaleId}`}>Klikk her for å fikse avtalen</Link>
        <br />
        <br />
      </>
    ) : null}
    Ta <a href={porten}>kontakt</a> i Porten dersom du trenger mer hjelp.
  </>
);

// På sikt burde denne egenskapen spesifiseres i skjema for gjennomføring, evt.
// utledes basert på eksplisitt styre-data i avtale eller tiltakstype.
function temporaryResolveOppstartstypeFromAvtale(
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

export const OpprettTiltaksgjennomforingContainer = (
  props: OpprettTiltaksgjennomforingContainerProps
) => {
  const { avtale, tiltaksgjennomforing, setError, onAvbryt } = props;
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
        tiltaksgjennomforing?.virksomhetsnummer || "",
      midlertidigStengt: {
        erMidlertidigStengt: Boolean(tiltaksgjennomforing?.stengtFra),
        stengtFra: tiltaksgjennomforing?.stengtFra
          ? new Date(tiltaksgjennomforing.stengtFra)
          : undefined,
        stengtTil: tiltaksgjennomforing?.stengtTil
          ? new Date(tiltaksgjennomforing.stengtTil)
          : undefined,
      },
    },
  });
  const {
    register,
    handleSubmit,
    formState: { errors },
    setValue,
    watch,
  } = form;

  const watchErMidlertidigStengt = watch(
    "midlertidigStengt.erMidlertidigStengt"
  );
  const { navigerTilTiltaksgjennomforing } =
    useNavigerTilTiltaksgjennomforing();

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
    if (ansatt && !isLoadingAnsatt) {
      setValue("ansvarlig", ansatt.ident!!);
    }
  }, [ansatt, isLoadingAnsatt, setValue]);

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
      virksomhetsnummer:
        data.tiltaksArrangorUnderenhetOrganisasjonsnummer ||
        tiltaksgjennomforing?.virksomhetsnummer ||
        "",
      tiltaksnummer: tiltaksgjennomforing?.tiltaksnummer,
      oppstart: temporaryResolveOppstartstypeFromAvtale(avtale),
      stengtFra: data.midlertidigStengt.erMidlertidigStengt
        ? formaterDatoSomYYYYMMDD(data.midlertidigStengt.stengtFra)
        : undefined,
      stengtTil: data.midlertidigStengt.erMidlertidigStengt
        ? formaterDatoSomYYYYMMDD(data.midlertidigStengt.stengtTil)
        : undefined,
    };

    try {
      const response =
        await mulighetsrommetClient.tiltaksgjennomforinger.opprettTiltaksgjennomforing(
          {
            requestBody: body,
          }
        );
      navigerTilTiltaksgjennomforing(response.id);
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

  if (!avtale?.navRegion) {
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

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <FormGroup>
          <TextField
            readOnly={arenaOpphav}
            error={errors.tittel?.message}
            label="Tiltaksnavn"
            {...register("tittel")}
          />
        </FormGroup>
        <FormGroup>
          <TextField readOnly label={"Avtale"} value={avtale?.navn || ""} />
        </FormGroup>
        <FormGroup>
          <FraTilDatoVelger
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
          <Checkbox {...register("midlertidigStengt.erMidlertidigStengt")}>
            Midlertidig stengt
          </Checkbox>
          {watchErMidlertidigStengt && (
            <FraTilDatoVelger
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
            label={"NAV region"}
            value={avtale?.navRegion?.navn || ""}
          />
          <ControlledMultiSelect
            placeholder={isLoadingEnheter ? "Laster enheter..." : "Velg en"}
            label={"NAV enhet (kontorer)"}
            {...register("navEnheter")}
            options={enheterOptions()}
          />
        </FormGroup>
        <FormGroup>
          <TextField
            label="Tiltaksarrangør hovedenhet"
            placeholder=""
            defaultValue={`${avtale?.leverandor.navn} - ${avtale?.leverandor.organisasjonsnummer}`}
            readOnly
          />
          <SokeSelect
            label="Tiltaksarrangør underenhet"
            placeholder="Velg underenhet for tiltaksarrangør"
            {...register("tiltaksArrangorUnderenhetOrganisasjonsnummer")}
            readOnly={!avtale?.leverandor.organisasjonsnummer}
            options={arrangorUnderenheterOptions()}
          />
        </FormGroup>
        <FormGroup>
          <SokeSelect
            placeholder={
              isLoadingAnsatt ? "Laster Tiltaksansvarlig..." : "Velg en"
            }
            label={"Tiltaksansvarlig"}
            {...register("ansvarlig")}
            options={
              isLoadingAnsatt
                ? [{ label: "Laster...", value: "" }]
                : [
                    {
                      value: ansatt?.ident ?? "",
                      label: `${navn} - ${ansatt?.ident}`,
                    },
                  ]
            }
          />
        </FormGroup>
        <Alert variant="warning" style={{ marginBottom: "1rem" }}>
          Opprettelse av gjennomføring her vil ikke opprette gjennomføringen i
          Arena.
        </Alert>
        <div className={styles.button_row}>
          <Button
            className={styles.button}
            onClick={onAvbryt}
            variant="tertiary"
            type="button"
          >
            Avbryt
          </Button>
          <Button className={styles.button} type="submit">
            {redigeringsModus ? "Lagre gjennomføring" : "Opprett"}
          </Button>
        </div>
      </form>
    </FormProvider>
  );
};
