import { Button, Select, TextField } from "@navikt/ds-react";
import classNames from "classnames";
import { ReactNode, useState } from "react";
import z from "zod";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Datovelger } from "../../skjema/OpprettComponents";
import styles from "./OpprettAvtaleContainer.module.scss";
import { useHentAnsatt } from "../../../api/ansatt/useHentAnsatt";
import { capitalize } from "../../../utils/Utils";
import { useAlleTiltakstyper } from "../../../api/tiltakstyper/useAlleTiltakstyper";
import { mulighetsrommetClient } from "../../../api/clients";
import { AvtaleRequest } from "mulighetsrommet-api-client/build/models/AvtaleRequest";
import { Avtaletype } from "mulighetsrommet-api-client/build/models/Avtaletype";
import { Avslutningsstatus } from "mulighetsrommet-api-client/build/models/Avslutningsstatus";

const Schema = z.object({
  avtalenavn: z.string().min(5, "Et avtalenavn må minst være 5 tegn langt"),
  tiltakstype: z.string({ required_error: "Du må velge en tiltakstype" }),
  avtaletype: z.string({ required_error: "Du må velge en avtaletype" }),
  leverandor: z.string().min(1, "Du må skrive inn en leverandør for avtalen"),
  enhet: z.string().min(1, "Du må velge en enhet"),
  antallPlasser: z
    .number({
      invalid_type_error:
        "Du må skrive inn antall plasser for avtalen som et tall",
    })
    .int(),
  fraDato: z.string({ required_error: "En avtale må ha en startdato" }),
  tilDato: z.string({ required_error: "En avtale må ha en sluttdato" }),
  avtaleansvarlig: z.string().min(1, "Du må velge en avtaleansvarlig"),
  url: z
    .string()
    .min(1, "Du må skrive inn url til avtalen i websak")
    .url("Ugyldig format på url"),
});

export type inferredSchema = z.infer<typeof Schema>;

export function OpprettAvtaleContainer() {
  return <ReactHookFormContainer />;
}

function ReactHookFormContainer() {
  const form = useForm<inferredSchema>({
    resolver: zodResolver(Schema),
  });
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = form;
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<string | null>(null);

  const postData: SubmitHandler<inferredSchema> = async (
    data
  ): Promise<void> => {
    setError(null);
    setResult(null);
    const postData: AvtaleRequest = {
      antallPlasser: data.antallPlasser,
      avtaletype: Avtaletype.FORHAANDSGODKJENT,
      enhet: data.enhet,
      leverandorOrganisasjonsnummer: data.leverandor,
      navn: data.avtalenavn,
      sluttDato: data.tilDato,
      startDato: data.fraDato,
      tiltakstypeId: data.tiltakstype,
      url: data.url,
      avslutningsstatus: Avslutningsstatus.IKKE_AVSLUTTET,
    };
    try {
      const response = await mulighetsrommetClient.avtaler.opprettAvtale({
        requestBody: postData,
      });
      setResult("hei");
    } catch {
      setError("Klarte ikke opprette avtale");
    }
  };
  const { data: ansatt, isLoading } = useHentAnsatt();
  const { data: tiltakstyper, isLoading: tiltakstyperLoading } =
    useAlleTiltakstyper();

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
            {tiltakstyperLoading && !tiltakstyper ? (
              <option value={""}>Laster...</option>
            ) : (
              tiltakstyper?.data
                ?.filter((tiltakstype) =>
                  ["VASV", "ARBFORB"].includes(tiltakstype.arenaKode)
                )
                .map((tiltakstype) => (
                  <option key={tiltakstype.id} value={tiltakstype.id}>
                    {tiltakstype.navn}
                  </option>
                ))
            )}
          </Select>
          <Select label={"Enhet"} {...register("enhet")}>
            {isLoading && !ansatt ? (
              <option value={""}>Laster...</option>
            ) : (
              <option value={ansatt?.hovedenhet}>
                {ansatt?.hovedenhetNavn}
              </option>
            )}
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
          <Select label={"Avtaletype"} {...register("avtaletype")}>
            <option value="forhandsgodkjent">Forhåndsgodkjent avtale</option>
          </Select>
          <TextField
            error={errors.url?.message}
            label="URL til avtale"
            {...register("url")}
          />
        </FormGroup>
        <FormGroup cols={2}>
          <Select label={"Avtaleansvarlig"} {...register("avtaleansvarlig")}>
            {isLoading && !ansatt ? (
              <option value={""}>Laster...</option>
            ) : (
              <option
                value={ansatt?.ident ?? "ukjent"}
              >{`${navn} - ${ansatt?.ident}`}</option>
            )}
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
