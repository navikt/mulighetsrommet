import { zodResolver } from "@hookform/resolvers/zod";
import { Button, TextField } from "@navikt/ds-react";
import {
  Avtale,
  NavEnhet,
  Norg2Type,
  Tiltaksgjennomforing,
  TiltaksgjennomforingRequest,
  Tiltakstypestatus,
} from "mulighetsrommet-api-client";
import { Dispatch, SetStateAction, useEffect, useState } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import z from "zod";
import { useAtom } from "jotai";
import { useHentAnsatt } from "../../../api/ansatt/useHentAnsatt";
import { avtaleFilter } from "../../../api/atoms";
import { useAvtale } from "../../../api/avtaler/useAvtale";
import { useAvtaler } from "../../../api/avtaler/useAvtaler";
import { mulighetsrommetClient } from "../../../api/clients";
import { useAlleEnheter } from "../../../api/enhet/useAlleEnheter";
import { capitalize, formaterDatoSomYYYYMMDD } from "../../../utils/Utils";
import { FormGroup } from "../../avtaler/opprett/OpprettAvtaleContainer";
import { Datovelger } from "../../skjema/OpprettComponents";
import { SokeSelect } from "../../skjema/SokeSelect";
import styles from "./OpprettTiltaksgjennomforingContainer.module.scss";
import { Laster } from "../../laster/Laster";
import { useNavigerTilTiltaksgjennomforing } from "../../../hooks/useNavigerTilTiltaksgjennomforing";
import { useTiltakstyper } from "../../../api/tiltakstyper/useTiltakstyper";
import { ControlledMultiSelect } from "../../skjema/ControlledMultiSelect";

const Schema = z.object({
  tiltakstype: z
    .string({ required_error: "Du må velge en tiltakstype" })
    .min(1),
  avtale: z.string({ required_error: "Du må velge en avtale" }).min(1),
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
  enheter: z
    .string()
    .array()
    .nonempty({ message: "Du må velge minst én enhet" }),
  ansvarlig: z.string({ required_error: "Du må velge en ansvarlig" }),
});

export type inferredSchema = z.infer<typeof Schema>;

interface OpprettTiltaksgjennomforingContainerProps {
  onAvbryt: () => void;
  setError: Dispatch<SetStateAction<boolean>>;
  setResult: Dispatch<SetStateAction<string | null>>;
  tiltaksgjennomforing?: Tiltaksgjennomforing;
}

export const OpprettTiltaksgjennomforingContainer = (
  props: OpprettTiltaksgjennomforingContainerProps
) => {
  const form = useForm<inferredSchema>({
    resolver: zodResolver(Schema),
    defaultValues: {
      tittel: props.tiltaksgjennomforing?.navn,
      tiltakstype: props.tiltaksgjennomforing?.tiltakstype?.id,
      enheter:
        props.tiltaksgjennomforing?.enheter.length === 0
          ? ["alle_enheter"]
          : props.tiltaksgjennomforing?.enheter,
      ansvarlig: props.tiltaksgjennomforing?.ansvarlig,
      avtale: props.tiltaksgjennomforing?.avtaleId,
      antallPlasser: props.tiltaksgjennomforing?.antallPlasser,
      startDato: props.tiltaksgjennomforing?.startDato
        ? new Date(props.tiltaksgjennomforing.startDato)
        : undefined,
      sluttDato: props.tiltaksgjennomforing?.sluttDato
        ? new Date(props.tiltaksgjennomforing.sluttDato)
        : undefined,
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
  const [aFilter, setAvtaleFilter] = useAtom(avtaleFilter);
  useEffect(() => {
    if (props.tiltaksgjennomforing?.tiltakstype) {
      setAvtaleFilter({
        ...aFilter,
        tiltakstype: props.tiltaksgjennomforing.tiltakstype.id,
      });
    }
  }, []);

  const { data: tiltakstyper, isError: isErrorTiltakstyper } = useTiltakstyper(
    { status: Tiltakstypestatus.AKTIV },
    1
  );

  const {
    data: enheter,
    isLoading: isLoadingEnheter,
    isError: isErrorEnheter,
  } = useAlleEnheter();

  const [avtaleId, setAvtaleId] = useState<string | undefined>(
    props.tiltaksgjennomforing?.avtaleId
  );
  const { data: avtale, isError: isErrorAvtale } = useAvtale(avtaleId);

  const {
    data: avtaler,
    isLoading: isLoadingAvtaler,
    isError: isErrorAvtaler,
  } = useAvtaler();

  const {
    data: ansatt,
    isLoading: isLoadingAnsatt,
    isError: isErrorAnsatt,
  } = useHentAnsatt();

  const [fylkeEnhet, setFylkeEnhet] = useState<NavEnhet | undefined>();

  useEffect(() => {
    if (ansatt && !isLoadingAnsatt) {
      setValue("ansvarlig", ansatt.ident!!);
    }
  }, [ansatt, isLoadingAnsatt, setValue]);

  const redigeringsModus = !!props.tiltaksgjennomforing;

  const postData: SubmitHandler<inferredSchema> = async (
    data
  ): Promise<void> => {
    const body: TiltaksgjennomforingRequest = {
      id: props.tiltaksgjennomforing ? props.tiltaksgjennomforing.id : uuidv4(),
      antallPlasser: data.antallPlasser,
      tiltakstypeId: data.tiltakstype,
      enheter: data.enheter.includes("alle_enheter") ? [] : data.enheter,
      navn: data.tittel,
      sluttDato: formaterDatoSomYYYYMMDD(data.sluttDato),
      startDato: formaterDatoSomYYYYMMDD(data.startDato),
      avtaleId: data.avtale,
      ansvarlig: data.ansvarlig,
      virksomhetsnummer: avtale!!.leverandor.organisasjonsnummer,
      tiltaksnummer: props.tiltaksgjennomforing?.tiltaksnummer,
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
      props.setError(true);
    }
  };

  const navn = ansatt?.fornavn
    ? [ansatt.fornavn, ansatt.etternavn ?? ""]
        .map((it) => capitalize(it))
        .join(" ")
    : "";

  if (!avtaler && !tiltakstyper && !enheter) {
    console.log("Laster...", { avtaler, tiltakstyper, enheter });
    return <Laster />;
  }

  if (
    isErrorAvtaler ||
    isErrorAnsatt ||
    isErrorAvtale ||
    isErrorTiltakstyper ||
    isErrorEnheter ||
    isErrorAvtale
  ) {
    props.setError(true);
  }

  const avtalerOptions = () => {
    if (avtale && !avtaler?.data.find((a) => a.id === avtale.id)) {
      avtaler?.data.push(avtale);
    }

    return avtaler
      ? avtaler?.data.map((avtale: Avtale) => ({
          value: avtale.id,
          label: avtale.navn,
        }))
      : [];
  };

  const overordnetEnhetFraAvtale = (): NavEnhet | undefined => {
    const avtaleEnhet = enheter?.find(
      (e: NavEnhet) => e.enhetNr === avtale?.navEnhet?.enhetsnummer
    );
    if (!avtaleEnhet) {
      return undefined;
    }
    return avtaleEnhet.overordnetEnhet
      ? enheter?.find(
          (e: NavEnhet) => e.overordnetEnhet === avtaleEnhet?.overordnetEnhet
        )
      : enheter?.find(
          (e: NavEnhet) => e.enhetNr === avtale?.navEnhet?.enhetsnummer
        );
  };

  const enheterLabel = () => {
    const overordnet = overordnetEnhetFraAvtale() ?? fylkeEnhet;
    return overordnet?.navn && overordnet.type === Norg2Type.FYLKE
      ? "Enheter i " + overordnet.navn
      : "Enheter";
  };

  const enheterOptions = () => {
    const overordnet = overordnetEnhetFraAvtale() ?? fylkeEnhet;

    const options = enheter
      ?.filter((enhet: NavEnhet) => {
        // Bare interessert i filtrering på overordnet enhet hvis overordnet enhet sin type er et fylke
        return overordnet?.type === Norg2Type.FYLKE
          ? overordnet.enhetNr === enhet.overordnetEnhet
          : true;
      })
      .map((enhet: NavEnhet) => ({
        label: enhet.navn,
        value: enhet.enhetNr,
      }));
    options?.unshift({ value: "alle_enheter", label: "Alle enheter" });
    return options || [];
  };

  const fylkeEnheterOptions = () => {
    const overordnedeEnhetsnummer = enheter
      ?.filter((enhet: NavEnhet) => enhet.overordnetEnhet)
      .map((enhet: NavEnhet) => enhet.overordnetEnhet);

    return (
      enheter
        ?.filter((enhet: NavEnhet) =>
          overordnedeEnhetsnummer?.includes(enhet.enhetNr)
        )
        .map((enhet: NavEnhet) => ({
          label: enhet.navn,
          value: enhet.enhetNr,
        })) ?? []
    );
  };

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <FormGroup>
          <SokeSelect
            placeholder="Velg en"
            label={"Tiltakstype"}
            {...register("tiltakstype", {
              onChange: (e) => {
                setAvtaleFilter({ ...aFilter, tiltakstype: e.target.value });
                setAvtaleId(undefined);
                form.resetField("avtale", { defaultValue: "" });
              },
            })}
            options={
              tiltakstyper
                ? tiltakstyper?.data.map((tiltakstype) => ({
                    value: tiltakstype.id,
                    label: tiltakstype.navn,
                  }))
                : []
            }
          />
          <SokeSelect
            placeholder={isLoadingAvtaler ? "Henter avtaler..." : "Velg en"}
            label="Avtale"
            {...register("avtale", {
              onChange: (e) => {
                setAvtaleId(e.target.value);
              },
            })}
            options={avtalerOptions()}
          />
        </FormGroup>
        <FormGroup>
          <TextField
            error={errors.tittel?.message}
            label="Tiltaksnavn"
            {...register("tittel")}
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
          <SokeSelect
            placeholder={overordnetEnhetFraAvtale()?.navn ?? "Velg en"}
            label={"Enhet (fylke)"}
            disabled={!!overordnetEnhetFraAvtale()}
            options={fylkeEnheterOptions()}
            defaultValue={overordnetEnhetFraAvtale()?.enhetNr}
            onChange={(e) => {
              setFylkeEnhet(
                enheter?.find((enhet: NavEnhet) => enhet.enhetNr === e)
              );
            }}
          />
          <ControlledMultiSelect
            placeholder={isLoadingEnheter ? "Laster enheter..." : "Velg en"}
            label={enheterLabel()}
            {...register("enheter")}
            options={enheterOptions()}
          />
          <SokeSelect
            placeholder={
              isLoadingAnsatt ? "Laster gjennomføringsansvarlig..." : "Velg en"
            }
            label={"Gjennomføringsansvarlig"}
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
        <FormGroup>
          <TextField
            readOnly
            style={{ backgroundColor: "#F1F1F1" }}
            label={"Arrangør - fra avtalen"}
            value={avtale?.leverandor.navn || ""}
          />
          <TextField
            readOnly
            style={{ backgroundColor: "#F1F1F1" }}
            label={"Avtaletype - fra avtalen"}
            value={avtale?.avtaletype || ""}
          />
        </FormGroup>
        <div className={styles.button_row}>
          <Button
            className={styles.button}
            onClick={props.onAvbryt}
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
