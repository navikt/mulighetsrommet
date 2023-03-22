import { Button, Select, Textarea, TextField } from "@navikt/ds-react";
import classNames from "classnames";
import { ReactNode } from "react";
import z from "zod";
import { useForm, SubmitHandler, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Datovelger } from "../../skjema/OpprettComponents";
import styles from "./OpprettAvtaleContainer.module.scss";
import { useHentAnsatt } from "../../../api/ansatt/useHentAnsatt";
import { Laster } from "../../laster/Laster";
import { capitalize } from "../../../utils/Utils";

const Schema = z.object({
  avtalenavn: z.string().min(5, "Et avtalenavn må minst være 5 tegn langt"),
  tiltakstype: z.string({ required_error: "Du må velge en tiltakstype" }),
  avtaletype: z.string({ required_error: "Du må velge en avtaletype" }),
  leverandor: z.string().min(1, "Du må skrive inn en leverandør for avtalen"),
  enhet: z.string({ required_error: "Du må velge en enhet" }),
  antallPlasser: z
    .number({ invalid_type_error: "Du må skrive inn et tall" })
    .int(),
  fraDato: z.string({ required_error: "En avtale må ha en startdato" }),
  tilDato: z.string({ required_error: "En avtale må ha en sluttdato" }),
  prisOgBetalingsbetingelser: z
    .string()
    .min(1, "Du må skrive inn pris og betalingsbetingelser"),
  avtaleansvarlig: z.string({
    required_error: "Du må velge en avtaleansvarlig",
  }),
  url: z.string().min(1, "Du må skrive inn url til avtalen i websak"),
});

export type inferredSchema = z.infer<typeof Schema>;

export function OpprettAvtaleContainer() {
  return (
    <div
      style={{
        width: "800px",
        margin: "0 auto",
        background: "white",
        padding: "1rem",
      }}
    >
      <ReactHookFormContainer />
    </div>
  );
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
  const onSubmit: SubmitHandler<inferredSchema> = (data) => console.log(data);
  const { data: ansatt, isLoading } = useHentAnsatt();
  if (isLoading) {
    return <Laster size="xlarge" />;
  }
  if (!ansatt) {
    return null;
  }
  const navn = ansatt.fornavn
    ? [ansatt.fornavn, ansatt.etternavn ?? ""]
        .map((it) => capitalize(it))
        .join(" ")
    : "";

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(onSubmit)}>
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
            <option value="VASV ">
              Varig tilrettelagt arbeid i skjermet virksomhet (VTA)
            </option>
            <option value="ARBFORB">Arbeidsforberedende trening (AFT)</option>
          </Select>
          <Select label={"Enhet"} {...register("enhet")}>
            <option value={ansatt.hovedenhet}>{ansatt.hovedenhetNavn}</option>
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
        <FormGroup cols={1}>
          <Textarea
            error={errors.prisOgBetalingsbetingelser?.message}
            label="Pris og betalingsbetingelser"
            size="medium"
            {...register("prisOgBetalingsbetingelser")}
          />
        </FormGroup>
        <FormGroup cols={2}>
          <Select label={"Avtaleansvarlig"} {...register("avtaleansvarlig")}>
            <option
              value={ansatt.ident ?? "ukjent"}
            >{`${navn} - ${ansatt.ident}`}</option>
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
