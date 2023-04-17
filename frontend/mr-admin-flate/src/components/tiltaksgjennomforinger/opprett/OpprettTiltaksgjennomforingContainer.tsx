import { Button, Select, TextField } from "@navikt/ds-react";
import { Dispatch, SetStateAction, useEffect, useState } from "react";
import z from "zod";
import { v4 as uuidv4 } from "uuid";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { capitalize, formaterDatoSomYYYYMMDD } from "../../../utils/Utils";
import { Datovelger } from "../../skjema/OpprettComponents";
import styles from "./OpprettTiltaksgjennomforingContainer.module.scss";
import { FormGroup } from "../../avtaler/opprett/OpprettAvtaleContainer";
import {
  Avtale,
  NavEnhet,
  Tiltaksgjennomforing,
  TiltaksgjennomforingRequest,
  Tiltakstypestatus,
} from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../../../api/clients";
import { useAtom } from "jotai";
import { avtaleFilter, tiltakstypefilter } from "../../../api/atoms";
import { useAvtaler } from "../../../api/avtaler/useAvtaler";
import { useAlleEnheter } from "../../../api/enhet/useAlleEnheter";
import { useHentAnsatt } from "../../../api/ansatt/useHentAnsatt";
import { useAvtale } from "../../../api/avtaler/useAvtale";
import { Laster } from "../../laster/Laster";
import useTiltakstyperWithFilter from "../../../api/tiltakstyper/useTiltakstyperWithFilter";
import { useNavigerTilTiltaksgjennomforing } from "../../../hooks/useNavigerTilTiltaksgjennomforing";

const Schema = z.object({
  tiltakstype: z.string().min(1, "Du må velge en tiltakstype"),
  avtale: z.string().min(1, "Du må velge en avtale"),
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
  enhet: z.string().min(1, "Du må velge en enhet"),
  ansvarlig: z.string().min(1, "Du må velge en ansvarlig"),
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
      enhet: props.tiltaksgjennomforing?.enhet,
      ansvarlig: props.tiltaksgjennomforing?.ansvarlig,
      avtale: props.tiltaksgjennomforing?.avtaleId,
      antallPlasser: props.tiltaksgjennomforing?.antallPlasser,
      startDato: props.tiltaksgjennomforing?.startDato ? new Date(props.tiltaksgjennomforing.startDato) : undefined,
      sluttDato: props.tiltaksgjennomforing?.sluttDato ? new Date(props.tiltaksgjennomforing.sluttDato) : undefined,
    },
  });
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = form;

  const { navigerTilTiltaksgjennomforing } = useNavigerTilTiltaksgjennomforing();
  const [aFilter, setAvtaleFilter] = useAtom(avtaleFilter);
  useEffect(() => {
    if (props.tiltaksgjennomforing?.tiltakstype) {
      setAvtaleFilter({ ...aFilter, tiltakstype: props.tiltaksgjennomforing.tiltakstype.id });
    }
  }, []);

  const [tFilter, setTiltakstypeFilter] = useAtom(tiltakstypefilter);
  useEffect(() => {
    setTiltakstypeFilter({ ...tFilter, status: Tiltakstypestatus.AKTIV });
  }, []);

  const {
    data: tiltakstyper,
    isLoading: isLoadingTiltakstyper,
    isError: isErrorTiltakstyper,
  } = useTiltakstyperWithFilter();

  const {
    data: enheter,
    isLoading: isLoadingEnheter,
    isError: isErrorEnheter,
  } = useAlleEnheter();

  const [avtaleId, setAvtaleId] = useState<string | undefined>(props.tiltaksgjennomforing?.avtaleId);
  const { data: avtale, isLoading: isLoadingAvtale, isError: isErrorAvtale } = useAvtale(avtaleId);

  const {
    data: avtaler,
    isLoading: isLoadingAvtaler,
    isError: isErrorAvtaler,
  } = useAvtaler();

  const { data: ansatt, isLoading: isLoadingAnsatt, isError: isErrorAnsatt } = useHentAnsatt();

  const redigeringsModus = !!props.tiltaksgjennomforing;

  const postData: SubmitHandler<inferredSchema> = async (
    data
  ): Promise<void> => {
    const body: TiltaksgjennomforingRequest = {
      id: props.tiltaksgjennomforing ? props.tiltaksgjennomforing.id : uuidv4(),
      antallPlasser: data.antallPlasser,
      tiltakstypeId: data.tiltakstype,
      enhet: data.enhet,
      navn: data.tittel,
      sluttDato: formaterDatoSomYYYYMMDD(data.sluttDato),
      startDato: formaterDatoSomYYYYMMDD(data.startDato),
      avtaleId: data.avtale,
      ansvarlig: data.ansvarlig,
      virksomhetsnummer: avtale?.leverandor?.organisasjonsnummer,
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

  if (isLoadingAvtaler || isLoadingAvtale || isLoadingAnsatt || isLoadingEnheter
    || isLoadingTiltakstyper || !avtaler || !tiltakstyper || !enheter
  ) {
    return <Laster />;
  }
  if (isErrorAvtaler || isErrorAnsatt || isErrorAvtale
    || isErrorTiltakstyper || isErrorEnheter || isErrorAvtale
  ) {
    props.setError(true);
  }
  
  const avtalerOptions = () => {
    if (avtale && !avtaler.data.find((a) => a.id === avtale.id)) {
      avtaler.data.push(avtale);
    }
    if (avtaler.data.length === 0) {
      return <option value={""}>Ingen avtaler funnet</option>;
    }
    return (
      <>
        <option value={""}>Velg en</option>
        {avtaler.data.map((avtale: Avtale) => (
          <option key={avtale.id} value={avtale.id}>
            {avtale.navn}
          </option>
        ))}
      </>
    );
  };

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <FormGroup>
          <Select
            label={"Tiltakstype"}
            error={errors.tiltakstype?.message}
            {...register("tiltakstype", {
              onChange: (e) => {
                setAvtaleFilter({ ...aFilter, tiltakstype: e.target.value });
                setAvtaleId(undefined);
                form.resetField("avtale", { defaultValue: "" })
              },
            })}
          >
            <>
              <option value={""}>Velg en</option>
              {tiltakstyper.data.map((tiltakstype) => (
                <option key={tiltakstype.id} value={tiltakstype.id}>
                  {tiltakstype.navn}
                </option>
              ))}
            </>
          </Select>
          <Select
            label={"Avtale"}
            error={errors.avtale?.message}
            {...register("avtale", {
              onChange: (e) => {
                setAvtaleId(e.target.value);
              },
            })}
          >
            {avtalerOptions()}
          </Select>
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
            style={{ width: "180px" }}
            label="Antall plasser"
            {...register("antallPlasser", { valueAsNumber: true })}
          />
        </FormGroup>
        <FormGroup>
          <Select
            label={"Enhet"}
            {...register("enhet")}
            error={errors.enhet?.message}
          >
            <>
              <option value={""}>Velg en</option>
              {enheter.map((enhet: NavEnhet) => (
                <option key={enhet.enhetNr} value={enhet.enhetNr}>
                  {enhet.navn}
                </option>
              ))}
            </>
          </Select>
          <Select
            error={errors.ansvarlig?.message}
            label={"Gjennomføringsansvarlig"}
            {...register("ansvarlig")}
          >
            {isLoadingAnsatt ?
              <option>Laster...</option>
              :
              <option
                value={ansatt?.ident ?? ""}
              >{`${navn} - ${ansatt?.ident}`}</option>
            }
          </Select>
        </FormGroup>
        <FormGroup>
          <TextField
            readOnly
            style={{ backgroundColor: "whitesmoke" }}
            label={"Arrangør - fra avtalen"}
            value={avtale?.leverandor?.navn || avtale?.leverandor?.navn || ""}
          />
          <TextField
            readOnly
            style={{ backgroundColor: "whitesmoke" }}
            label={"Avtaletype - fra avtalen"}
            value={avtale?.avtaletype || avtale?.avtaletype || ""}
          />
        </FormGroup>
        <div className={styles.button_row}>
          <Button className={styles.button} onClick={props.onAvbryt} variant="tertiary">
            Avbryt
          </Button>
          <Button className={styles.button} type="submit">
            { redigeringsModus ? "Lagre gjennomforing" : "Opprett"}
          </Button>
        </div>
      </form>
    </FormProvider>
  );
};
