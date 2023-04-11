import { zodResolver } from "@hookform/resolvers/zod";
import { Button, Select, TextField } from "@navikt/ds-react";
import classNames from "classnames";
import { Avtale, AvtaleRequest, Avtaletype } from "mulighetsrommet-api-client";
import { Ansatt } from "mulighetsrommet-api-client/build/models/Ansatt";
import { NavEnhet } from "mulighetsrommet-api-client/build/models/NavEnhet";
import { Tiltakstype } from "mulighetsrommet-api-client/build/models/Tiltakstype";
import { StatusModal } from "mulighetsrommet-veileder-flate/src/components/modal/delemodal/StatusModal";
import { porten } from "mulighetsrommet-veileder-flate/src/constants";
import { Dispatch, ReactNode, SetStateAction, useState } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import z from "zod";
import { mulighetsrommetClient } from "../../../api/clients";
import { capitalize, formaterDatoSomYYYYMMDD } from "../../../utils/Utils";
import { Datovelger } from "../../skjema/OpprettComponents";
import styles from "./OpprettAvtaleContainer.module.scss";
import { useNavigerTilAvtale } from "../../../hooks/useNavigerTilAvtale";

interface OpprettAvtaleContainerProps {
  setResult: Dispatch<SetStateAction<string | null>>;
  tiltakstyper: Tiltakstype[];
  ansatt: Ansatt;
  avtale?: Avtale;
  enheter: NavEnhet[];
}

const GyldigUrlHvisVerdi = z.union([
  z.literal(""),
  z.string().trim().url("Du må skrive inn en gyldig nettadresse"),
]);

const Schema = z.object({
  avtalenavn: z.string().min(5, "Et avtalenavn må minst være 5 tegn langt"),
  tiltakstype: z.string().min(1, "Du må velge en tiltakstype"),
  avtaletype: z.nativeEnum(Avtaletype, {
    required_error: "Du må velge en avtaletype",
  }),
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
    .gt(0, "Antall plasser må være større enn 0")
    .int(),
  fraDato: z
    .date({ required_error: "En avtale må ha en startdato" })
    .nullable(),
  tilDato: z
    .date({ required_error: "En avtale må ha en sluttdato" })
    .nullable(),
  avtaleansvarlig: z.string().min(1, "Du må velge en avtaleansvarlig"),
  url: GyldigUrlHvisVerdi,
});

export type inferredSchema = z.infer<typeof Schema>;

export function OpprettAvtaleContainer({
  setResult,
  tiltakstyper,
  ansatt,
  enheter,
  avtale,
}: OpprettAvtaleContainerProps) {
  const { navigerTilAvtale } = useNavigerTilAvtale();
  const redigeringsModus = !!avtale;
  const [feil, setFeil] = useState<string | null>("");
  const clickCancel = () => {
    setFeil(null);
  };

  console.log(avtale);
  const form = useForm<inferredSchema>({
    resolver: zodResolver(Schema),
    defaultValues: {
      tiltakstype: avtale?.tiltakstype?.id,
      enhet: avtale?.navEnhet?.enhetsnummer ?? ansatt.hovedenhet,
      avtaleansvarlig: avtale?.ansvarlig || ansatt?.ident || "",
      avtalenavn: avtale?.navn || "",
      avtaletype: avtale?.avtaletype || "",
      leverandor: avtale?.leverandor?.organisasjonsnummer || "",
      antallPlasser: avtale?.antallPlasser || 0,
      fraDato: avtale?.startDato ? new Date(avtale.startDato) : null,
      tilDato: avtale?.sluttDato ? new Date(avtale.sluttDato) : null,
      url: avtale?.url || "",
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
    setFeil(null);
    setResult(null);

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
      avtaletype: data.avtaletype,
    };

    if (avtale?.id) {
      postData.id = avtale.id; // Ved oppdatering av eksisterende avtale
    }

    try {
      const response = await mulighetsrommetClient.avtaler.opprettAvtale({
        requestBody: postData,
      });
      navigerTilAvtale(response.id);
      return;
    } catch {
      setFeil("Klarte ikke opprette eller redigere avtale");
    }
  };

  const navn = ansatt?.fornavn
    ? [ansatt.fornavn, ansatt.etternavn ?? ""]
        .map((it) => capitalize(it))
        .join(" ")
    : "";

  if (feil) {
    return (
      <StatusModal
        modalOpen={!!feil}
        ikonVariant="error"
        heading="Kunne ikke opprette avtale"
        text={
          <>
            Avtalen kunne ikke opprettes på grunn av en teknisk feil hos oss.
            Forsøk på nytt eller ta <a href={porten}>kontakt</a> i Porten dersom
            du trenger mer hjelp.
          </>
        }
        onClose={clickCancel}
        primaryButtonOnClick={() => setFeil("")}
        primaryButtonText="Prøv igjen"
        secondaryButtonOnClick={clickCancel}
        secondaryButtonText="Avbryt"
      />
    );
  }

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
            defaultValue={ansatt.hovedenhet}
          >
            {enheter.map((enhet) => (
              <option key={enhet.enhetId} value={enhet.enhetNr}>
                {enhet.navn}
              </option>
            ))}
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
            <option value={Avtaletype.FORHAANDSGODKJENT}>
              Forhåndsgodkjent avtale
            </option>
            <option value={Avtaletype.RAMMEAVTALE}>Rammeavtale</option>
            <option value={Avtaletype.AVTALE}>Avtale</option>
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
          <Button type="submit">
            {redigeringsModus ? "Lagre redigert avtale" : "Registrer avtale"}{" "}
          </Button>
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
