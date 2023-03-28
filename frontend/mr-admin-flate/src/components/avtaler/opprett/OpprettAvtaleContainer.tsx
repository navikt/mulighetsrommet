import { Button, Select, TextField } from "@navikt/ds-react";
import classNames from "classnames";
import { Dispatch, ReactNode, SetStateAction, useState } from "react";
import z from "zod";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Datovelger } from "../../skjema/OpprettComponents";
import styles from "./OpprettAvtaleContainer.module.scss";
import { capitalize, formaterDatoSomYYYYMMDD } from "../../../utils/Utils";
import { mulighetsrommetClient } from "../../../api/clients";
import { AvtaleRequest } from "mulighetsrommet-api-client/build/models/AvtaleRequest";
import { Avtaletype } from "mulighetsrommet-api-client/build/models/Avtaletype";
import { Ansatt } from "mulighetsrommet-api-client/build/models/Ansatt";
import { Tiltakstype } from "mulighetsrommet-api-client/build/models/Tiltakstype";

interface OpprettAvtaleContainerProps {
  setError: Dispatch<SetStateAction<string | null>>;
  setResult: Dispatch<SetStateAction<string | null>>;
  tiltakstyper: Tiltakstype[];
  ansatt: Ansatt;
}

const Schema = z.object({
  avtalenavn: z.string().min(5, "Et avtalenavn må minst være 5 tegn langt"),
  tiltakstype: z.string().min(1, "Du må velge en tiltakstype"),
  avtaletype: z.string({ required_error: "Du må velge en avtaletype" }),
  leverandor: z
    .string()
    .min(9, "Organisasjonsnummer må være 9 siffer")
    .max(9, "Organisasjonsnummer må være 9 siffer")
    .regex(/^\d+$/, "Leverandør må være et nummer"),
  enhet: z.string().min(1, "Du må velge en enhet"),
  antallPlasser: z
    .number({
      invalid_type_error:
        "Du må skrive inn antall plasser for avtalen som et tall",
    })
    .int(),
  fraDato: z.date({ required_error: "En avtale må ha en startdato" }),
  tilDato: z.date({ required_error: "En avtale må ha en sluttdato" }),
  avtaleansvarlig: z.string().min(1, "Du må velge en avtaleansvarlig"),
  url: z
    .string()
    .min(1, "Du må skrive inn url til avtalen i websak")
    .url("Ugyldig format på url"),
});

export type inferredSchema = z.infer<typeof Schema>;

export function OpprettAvtaleContainer({
  setError,
  setResult,
  tiltakstyper,
  ansatt,
}: OpprettAvtaleContainerProps) {
  const form = useForm<inferredSchema>({
    resolver: zodResolver(Schema),
    defaultValues: {
      tiltakstype: tiltakstyper[0].id,
      enhet: ansatt.hovedenhet,
      avtaleansvarlig: ansatt.ident ?? "",
    },
  });
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = form;

  const postData: SubmitHandler<inferredSchema> = async (
    data
  ): Promise<void> => {
    setError(null);
    setResult(null);

    console.log(formaterDatoSomYYYYMMDD(data.fraDato));
    console.log();
    const postData: AvtaleRequest = {
      antallPlasser: data.antallPlasser,
      enhet: data.enhet,
      leverandorOrganisasjonsnummer: data.leverandor,
      navn: data.avtalenavn,
      sluttDato: formaterDatoSomYYYYMMDD(data.tilDato),
      startDato: formaterDatoSomYYYYMMDD(data.fraDato),
      tiltakstypeId: data.tiltakstype,
      url: data.url,
      ansvarlig: data.avtaleansvarlig,
    };

    try {
      const response = await mulighetsrommetClient.avtaler.opprettAvtale({
        requestBody: postData,
      });
      setResult(response.id);
    } catch {
      setError("Klarte ikke opprette avtale");
    }
  };

  const navn = ansatt?.fornavn
    ? [ansatt.fornavn, ansatt.etternavn ?? ""]
        .map((it) => capitalize(it))
        .join(" ")
    : "";

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <FormGroup>
          <TextField
            error={errors.avtalenavn?.message}
            label="Avtalenavn"
            {...register("avtalenavn")}
          />
        </FormGroup>
        <FormGroup>
          <Datovelger
            fra={{
              label: "Start",
              error: errors.fraDato?.message,
              ...register("fraDato"),
            }}
            til={{
              label: "Slutt",
              error: errors.tilDato?.message,
              ...register("tilDato"),
            }}
          />
        </FormGroup>
        <FormGroup cols={2}>
          <Select label={"Tiltakstype"} {...register("tiltakstype")}>
            {tiltakstyper.map((tiltakstype) => (
              <option key={tiltakstype.id} value={tiltakstype.id}>
                {tiltakstype.navn}
              </option>
            ))}
          </Select>

          <Select
            error={errors.enhet?.message}
            label={"Enhet"}
            {...register("enhet")}
          >
            <option value={ansatt?.hovedenhet}>{ansatt?.hovedenhetNavn}</option>
          </Select>
          <TextField
            error={errors.antallPlasser?.message}
            label="Antall plasser"
            {...register("antallPlasser", { valueAsNumber: true })}
          />
          <TextField
            error={errors.leverandor?.message}
            label="Leverandør"
            {...register("leverandor")}
          />
          <Select
            error={errors.avtaletype?.message}
            label={"Avtaletype"}
            {...register("avtaletype")}
          >
            <option value="forhandsgodkjent">Forhåndsgodkjent avtale</option>
          </Select>
          <TextField
            error={errors.url?.message}
            label="URL til avtale"
            {...register("url")}
          />
        </FormGroup>
        <FormGroup cols={2}>
          <Select
            error={errors.avtaleansvarlig?.message}
            label={"Avtaleansvarlig"}
            {...register("avtaleansvarlig")}
          >
            <option
              value={ansatt.ident ?? ""}
            >{`${navn} - ${ansatt?.ident}`}</option>
          </Select>
        </FormGroup>
        <div className={styles.content_right}>
          <Button type="submit">Registrer avtale</Button>
        </div>
      </form>
    </FormProvider>
  );
}

function FormGroup({
  children,
  cols = 1,
}: {
  children: ReactNode;
  cols?: number;
}) {
  return (
    <div
      className={classNames(styles.form_group, styles.grid, {
        [styles.grid_1]: cols === 1,
        [styles.grid_2]: cols === 2,
      })}
    >
      {children}
    </div>
  );
}
