import { zodResolver } from "@hookform/resolvers/zod";
import { Button, TextField } from "@navikt/ds-react";
import {
  Avtale,
  NavEnhet,
  Tiltaksgjennomforing,
  TiltaksgjennomforingRequest,
} from "mulighetsrommet-api-client";
import React, { Dispatch, SetStateAction, useEffect } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import z from "zod";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { mulighetsrommetClient } from "../../api/clients";
import { useAlleEnheter } from "../../api/enhet/useAlleEnheter";
import { Datovelger } from "../skjema/Datovelger";
import { SokeSelect } from "../skjema/SokeSelect";
import styles from "./OpprettTiltaksgjennomforingContainer.module.scss";
import { Laster } from "../laster/Laster";
import { useNavigerTilTiltaksgjennomforing } from "../../hooks/useNavigerTilTiltaksgjennomforing";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FormGroup } from "../avtaler/OpprettAvtaleContainer";
import { porten } from "mulighetsrommet-frontend-common/constants";
import { capitalize, formaterDatoSomYYYYMMDD } from "../../utils/Utils";
import { useVirksomhet } from "../../api/virksomhet/useVirksomhet";

const Schema = z.object({
  tittel: z.string().min(1, "Du må skrive inn tittel"),
  startDato: z.date({ required_error: "En gjennomføring må ha en startdato" }),
  sluttDato: z.date({ required_error: "En gjennomføring må ha en sluttdato" }),
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
});

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

const avtaleManglerNavRegionError = () => (
  <>
    Avtalen mangler NAV region. Du må oppdatere avtalens NAV region for å kunne
    opprette en gjennomføring. Ta <a href={porten}>kontakt</a> i Porten dersom
    du trenger mer hjelp.
  </>
);

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
          : tiltaksgjennomforing?.navEnheter,
      ansvarlig: tiltaksgjennomforing?.ansvarlig,
      antallPlasser: tiltaksgjennomforing?.antallPlasser,
      startDato: tiltaksgjennomforing?.startDato
        ? new Date(tiltaksgjennomforing.startDato)
        : undefined,
      sluttDato: tiltaksgjennomforing?.sluttDato
        ? new Date(tiltaksgjennomforing.sluttDato)
        : undefined,
      tiltaksArrangorUnderenhetOrganisasjonsnummer:
        tiltaksgjennomforing?.virksomhetsnummer || "",
    },
  });
  const {
    register,
    handleSubmit,
    formState: { errors },
    setValue,
  } = form;

  const { navigerTilTiltaksgjennomforing } =
    useNavigerTilTiltaksgjennomforing();

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
    const body: TiltaksgjennomforingRequest = {
      id: tiltaksgjennomforing ? tiltaksgjennomforing.id : uuidv4(),
      antallPlasser: data.antallPlasser,
      tiltakstypeId: avtale?.tiltakstype.id || "",
      navEnheter: data.navEnheter.includes("alle_enheter")
        ? []
        : data.navEnheter,
      navn: data.tittel,
      sluttDato: formaterDatoSomYYYYMMDD(data.sluttDato),
      startDato: formaterDatoSomYYYYMMDD(data.startDato),
      avtaleId: avtale?.id || "",
      ansvarlig: data.ansvarlig,
      virksomhetsnummer:
        data.tiltaksArrangorUnderenhetOrganisasjonsnummer ||
        tiltaksgjennomforing?.virksomhetsnummer ||
        "",
      tiltaksnummer: tiltaksgjennomforing?.tiltaksnummer,
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

  const navn = ansatt?.fornavn
    ? [ansatt.fornavn, ansatt.etternavn ?? ""]
        .map((it) => capitalize(it))
        .join(" ")
    : "";

  if (!enheter) {
    return <Laster />;
  }

  if (!avtale?.navRegion) {
    setError(avtaleManglerNavRegionError());
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
          avtale?.navEnheter.includes(enhet.enhetsnummer)
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
            error={errors.tittel?.message}
            label="Tiltaksnavn"
            {...register("tittel")}
          />
        </FormGroup>
        <FormGroup>
          <TextField
            readOnly
            style={{ backgroundColor: "#F1F1F1" }}
            label={"Avtale"}
            value={avtale?.navn || ""}
          />
        </FormGroup>
        <FormGroup>
          <Datovelger
            fra={{
              label: "Startdato",
              error: errors.startDato?.message,
              ...register("startDato"),
            }}
            til={{
              label: "Sluttdato",
              error: errors.sluttDato?.message,
              ...register("sluttDato"),
            }}
          />
          <TextField
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
            style={{ backgroundColor: "#F1F1F1" }}
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
            style={{ backgroundColor: "#F1F1F1" }}
            readOnly
          />
          <SokeSelect
            label="Tiltaksarrangør underenhet"
            placeholder="Velg underenhet for tiltaksarrangør"
            {...register("tiltaksArrangorUnderenhetOrganisasjonsnummer")}
            disabled={!avtale?.leverandor.organisasjonsnummer}
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
