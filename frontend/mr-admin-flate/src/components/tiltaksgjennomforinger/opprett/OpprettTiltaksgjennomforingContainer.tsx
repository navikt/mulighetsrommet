import { Button, Select, TextField } from "@navikt/ds-react";
import { Dispatch, SetStateAction, useEffect, useState } from "react";
import z from "zod";
import { v4 as uuidv4 } from "uuid";
import { useForm, SubmitHandler, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { formaterDatoSomYYYYMMDD } from "../../../utils/Utils";
import { Datovelger } from "../../skjema/OpprettComponents";
import styles from "./OpprettTiltaksgjennomforingContainer.module.scss";
import { FormGroup } from "../../avtaler/opprett/OpprettAvtaleContainer";
import {
  Avtale,
  NavEnhet,
  TiltaksgjennomforingRequest,
  Tiltakstypestatus,
} from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../../../api/clients";
import { useAtom } from "jotai";
import { avtaleFilter, tiltakstypefilter } from "../../../api/atoms";
import { useAvtaler } from "../../../api/avtaler/useAvtaler";
import { useAlleEnheter } from "../../../api/enhet/useAlleEnheter";
import useTiltakstyperWithFilter from "../../../api/tiltakstyper/useTiltakstyperWithFilter";

const Schema = z.object({
  tiltakstype: z.string().min(1, "Du må velge en tiltakstype"),
  avtale: z.string().min(1, "Du må velge en avtale"),
  tittel: z.string().min(1, "Du må skrive inn tittel"),
  tiltaksnummer: z
    .number({ invalid_type_error: "Du må skrive inn et tall" })
    .int(),
  aar: z.number({ invalid_type_error: "Du må skrive inn et tall" }).int(),
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
  tiltakssted: z.string({
    required_error: "Du må skrive inn tiltakssted for gjennomføringen",
  }),
});

export type inferredSchema = z.infer<typeof Schema>;

interface OpprettTiltaksgjennomforingContainerProps {
  onAvbryt: () => void;
  setError: Dispatch<SetStateAction<boolean>>;
  setResult: Dispatch<SetStateAction<string | null>>;
}

export const OpprettTiltaksgjennomforingContainer = (
  props: OpprettTiltaksgjennomforingContainerProps
) => {
  const form = useForm<inferredSchema>({
    resolver: zodResolver(Schema),
  });
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = form;

  const [aFilter, setAvtaleFilter] = useAtom(avtaleFilter);
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
  const {
    data: avtaler,
    isLoading: isLoadingAvtaler,
    isError: isErrorAvtaler,
  } = useAvtaler();
  if (isErrorAvtaler || isErrorEnheter || isErrorTiltakstyper) {
    props.setError(true);
  }

  const [avtale, setAvtale] = useState<Avtale | undefined>(undefined);

  const postData: SubmitHandler<inferredSchema> = async (
    data
  ): Promise<void> => {
    props.setError(false);
    props.setResult(null);
    if (!avtale?.leverandor?.organisasjonsnummer) {
      props.setError(true);
      return;
    }

    const body: TiltaksgjennomforingRequest = {
      id: uuidv4(),
      antallPlasser: data.antallPlasser,
      tiltakstypeId: data.tiltakstype,
      enhet: data.enhet,
      navn: data.tittel,
      sluttDato: formaterDatoSomYYYYMMDD(data.startDato),
      startDato: formaterDatoSomYYYYMMDD(data.sluttDato),
      avtaleId: data.avtale,
      virksomhetsnummer: avtale?.leverandor?.organisasjonsnummer,
      tiltaksnummer: `${data.tiltaksnummer}#${data.aar}`,
    };

    try {
      const response =
        await mulighetsrommetClient.tiltaksgjennomforinger.opprettTiltaksgjennomforing(
          {
            requestBody: body,
          }
        );
      props.setResult(response.id);
    } catch {
      props.setError(true);
    }
  };

  const avtalerOptions = () => {
    if (isLoadingAvtaler || !avtaler) {
      return <option value={""}>Laster...</option>;
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
                setAvtale(undefined);
              },
            })}
          >
            {isLoadingTiltakstyper || !tiltakstyper ? (
              <option value={""}>Laster...</option>
            ) : (
              <>
                <option value={""}>Velg en</option>
                {tiltakstyper.data.map((tiltakstype) => (
                  <option key={tiltakstype.id} value={tiltakstype.id}>
                    {tiltakstype.navn}
                  </option>
                ))}
              </>
            )}
          </Select>
          <Select
            label={"Avtale"}
            error={errors.avtale?.message}
            {...register("avtale", {
              onChange: (e) => {
                const selectedAvtale = avtaler?.data.find(
                  (avtale: Avtale) => avtale.id === e.target.value
                );
                setAvtale(selectedAvtale);
              },
            })}
          >
            {avtalerOptions()}
          </Select>
        </FormGroup>
        <FormGroup cols={2}>
          <TextField
            error={errors.tittel?.message}
            label="Tiltaksnavn"
            {...register("tittel")}
          />
          <div></div>
          <TextField
            error={errors.tiltaksnummer?.message}
            label="Tiltaksnummer"
            {...register("tiltaksnummer", { valueAsNumber: true })}
          />
          <TextField
            error={errors.aar?.message}
            label="År"
            {...register("aar", { valueAsNumber: true })}
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
            style={{ width: "163px" }}
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
            {" "}
            {isLoadingEnheter || !enheter ? (
              <option value={""}>Laster...</option>
            ) : (
              <>
                <option value={""}>Velg en</option>
                {enheter?.map((enhet: NavEnhet) => (
                  <option key={enhet.enhetId} value={enhet.enhetId}>
                    {enhet.navn}
                  </option>
                ))}
              </>
            )}
          </Select>
          <TextField
            error={errors.tiltakssted?.message}
            label="Tiltakssted"
            {...register("tiltakssted")}
          />
        </FormGroup>
        <FormGroup>
          <TextField
            readOnly
            label={"Arrangør - fra avtalen"}
            value={avtale?.leverandor?.navn || ""}
          />
          <TextField
            readOnly
            label={"Avtaletype - fra avtalen"}
            value={avtale?.avtaletype || ""}
          />
        </FormGroup>
        <div className={styles.button_row}>
          <Button onClick={props.onAvbryt} variant="danger">Avbryt</Button>
          <Button type="submit">Opprett</Button>
        </div>
      </form>
    </FormProvider>
  );
};
