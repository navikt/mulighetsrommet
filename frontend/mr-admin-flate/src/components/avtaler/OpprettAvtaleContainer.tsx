import { zodResolver } from "@hookform/resolvers/zod";
import { Button, Textarea, TextField } from "@navikt/ds-react";
import classNames from "classnames";
import { Avtale, AvtaleRequest, Avtaletype, Norg2Type } from "mulighetsrommet-api-client";
import { Ansatt } from "mulighetsrommet-api-client/build/models/Ansatt";
import { NavEnhet } from "mulighetsrommet-api-client/build/models/NavEnhet";
import { Tiltakstype } from "mulighetsrommet-api-client/build/models/Tiltakstype";
import { StatusModal } from "mulighetsrommet-veileder-flate/src/components/modal/delemodal/StatusModal";
import { porten } from "mulighetsrommet-frontend-common/constants";
import { Dispatch, ReactNode, SetStateAction, useState } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import z from "zod";
import { mulighetsrommetClient } from "../../api/clients";
import {
  capitalize,
  formaterDatoSomYYYYMMDD,
  tiltakstypekodeErAnskaffetTiltak,
} from "../../utils/Utils";
import { Datovelger } from "../skjema/Datovelger";
import { VirksomhetInput } from "../virksomhet/VirksomhetInput";
import styles from "./OpprettAvtaleContainer.module.scss";
import { useNavigerTilAvtale } from "../../hooks/useNavigerTilAvtale";
import { SokeSelect } from "../skjema/SokeSelect";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";

interface OpprettAvtaleContainerProps {
  onAvbryt: () => void;
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
  tiltakstype: z.string({ required_error: "Du må velge en tiltakstype" }),
  avtaletype: z.nativeEnum(Avtaletype, {
    required_error: "Du må velge en avtaletype",
  }),
  leverandor: z
    .string()
    .min(9, "Organisasjonsnummer må være 9 siffer")
    .max(9, "Organisasjonsnummer må være 9 siffer")
    .regex(/^\d+$/, "Leverandør må være et nummer"),
  navRegion: z.string({ required_error: "Du må velge en enhet" }),
  navEnheter: z
    .string()
    .array()
    .nonempty({ message: "Du må velge minst én enhet" }),
  antallPlasser: z
    .number({
      invalid_type_error:
        "Du må skrive inn antall plasser for avtalen som et tall",
    })
    .gt(0, "Antall plasser må være større enn 0")
    .int(),
  startDato: z
    .date({ required_error: "En avtale må ha en startdato" })
    .nullable(),
  sluttDato: z
    .date({ required_error: "En avtale må ha en sluttdato" })
    .nullable(),
  avtaleansvarlig: z.string({
    required_error: "Du må velge en avtaleansvarlig",
  }),
  url: GyldigUrlHvisVerdi,
  prisOgBetalingsinfo: z.string().optional(),
});

export type inferredSchema = z.infer<typeof Schema>;

export function OpprettAvtaleContainer({
  onAvbryt,
  setResult,
  tiltakstyper,
  ansatt,
  enheter,
  avtale,
}: OpprettAvtaleContainerProps) {
  const { navigerTilAvtale } = useNavigerTilAvtale();
  const redigeringsModus = !!avtale;
  const [feil, setFeil] = useState<string | null>("");
  const [navRegion, setNavRegion] = useState<string | undefined>(avtale?.navRegion?.enhetsnummer);

  const clickCancel = () => {
    setFeil(null);
  };

  const defaultEnhet = () => {
    if (avtale?.navRegion?.enhetsnummer) {
      return avtale?.navRegion?.enhetsnummer;
    }
    if (enheter.find((e) => e.enhetsnummer === ansatt.hovedenhet)) {
      return ansatt.hovedenhet;
    }
    return undefined;
  };

  const form = useForm<inferredSchema>({
    resolver: zodResolver(Schema),
    defaultValues: {
      tiltakstype: avtale?.tiltakstype?.id,
      navRegion: defaultEnhet(),
      navEnheter:
        avtale?.navEnheter.length === 0
          ? ["alle_enheter"]
          : avtale?.navEnheter,
      avtaleansvarlig: avtale?.ansvarlig || ansatt?.ident || "",
      avtalenavn: avtale?.navn || "",
      avtaletype: avtale?.avtaletype || Avtaletype.AVTALE,
      leverandor: avtale?.leverandor?.organisasjonsnummer || "",
      antallPlasser: avtale?.antallPlasser || 0,
      startDato: avtale?.startDato ? new Date(avtale.startDato) : null,
      sluttDato: avtale?.sluttDato ? new Date(avtale.sluttDato) : null,
      url: avtale?.url || "",
      prisOgBetalingsinfo: avtale?.prisbetingelser || undefined,
    },
  });
  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
  } = form;

  const erAnskaffetTiltak = (tiltakstypeId: string): boolean => {
    const tiltakstype = tiltakstyper.find((type) => type.id === tiltakstypeId);
    return tiltakstypekodeErAnskaffetTiltak(tiltakstype?.arenaKode);
  };

  const postData: SubmitHandler<inferredSchema> = async (
    data
  ): Promise<void> => {
    setFeil(null);
    setResult(null);

    const postData: AvtaleRequest = {
      antallPlasser: data.antallPlasser,
      navRegion: data.navRegion,
      navEnheter: data.navEnheter.includes("alle_enheter") ? [] : data.navEnheter,
      avtalenummer: avtale?.avtalenummer || "",
      leverandorOrganisasjonsnummer: data.leverandor,
      navn: data.avtalenavn,
      sluttDato: formaterDatoSomYYYYMMDD(data.sluttDato),
      startDato: formaterDatoSomYYYYMMDD(data.startDato),
      tiltakstypeId: data.tiltakstype,
      url: data.url,
      ansvarlig: data.avtaleansvarlig,
      avtaletype: data.avtaletype,
      prisOgBetalingsinformasjon: erAnskaffetTiltak(data.tiltakstype)
        ? data.prisOgBetalingsinfo
        : undefined,
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

  const enheterOptions = () => {
    if (!navRegion) {
      return [];
    }

    const options = enheter
      ?.filter((enhet: NavEnhet) => {
        return navRegion === enhet.overordnetEnhet
      })
      .map((enhet: NavEnhet) => ({
        label: enhet.navn,
        value: enhet.enhetsnummer,
      }));
    options?.unshift({ value: "alle_enheter", label: "Alle enheter" });
    return options || [];
  };

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
        <FormGroup cols={2}>
          <SokeSelect
            placeholder="Velg en"
            label={"Tiltakstype"}
            {...register("tiltakstype")}
            options={tiltakstyper.map((tiltakstype) => ({
              value: tiltakstype.id,
              label: tiltakstype.navn,
            }))}
          />
          <div></div>
          <SokeSelect
            placeholder="Velg en"
            label={"Avtaletype"}
            {...register("avtaletype")}
            options={[
              {
                value: Avtaletype.FORHAANDSGODKJENT,
                label: "Forhåndsgodkjent avtale",
              },
              {
                value: Avtaletype.RAMMEAVTALE,
                label: "Rammeavtale",
              },
              {
                value: Avtaletype.AVTALE,
                label: "Avtale",
              },
            ]}
          />
          <TextField
            type={"number"}
            error={errors.antallPlasser?.message}
            label="Antall plasser"
            {...register("antallPlasser", { valueAsNumber: true })}
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
        </FormGroup>
        <FormGroup>
         <SokeSelect
            placeholder="Velg en"
            label={"NAV region"}
            {...register("navRegion")}
            onChange={(e) => {
              setNavRegion(e);
              form.setValue("navEnheter", [] as any);
            }}
            options={enheter
              .filter((enhet) => enhet.type === Norg2Type.FYLKE)
              .map((enhet) => ({
                value: `${enhet.enhetsnummer}`,
                label: enhet.navn,
              }))}
          />
          <ControlledMultiSelect
            placeholder="Velg en"
            disabled={!navRegion}
            label={"NAV enhet (kontorer)"}
            {...register("navEnheter")}
            options={enheterOptions()}
          />
        </FormGroup>
        <FormGroup>
          <VirksomhetInput avtale={avtale} />
          <TextField
            error={errors.url?.message}
            label="URL til avtale"
            {...register("url")}
          />
        </FormGroup>
        {erAnskaffetTiltak(watch("tiltakstype")) ? (
          <FormGroup>
            <Textarea
              error={errors.prisOgBetalingsinfo?.message}
              label="Pris og betalingsinformasjon"
              {...register("prisOgBetalingsinfo")}
            />
          </FormGroup>
        ) : null}
        <FormGroup cols={2}>
          <SokeSelect
            placeholder="Velg en"
            label={"Avtaleansvarlig"}
            {...register("avtaleansvarlig")}
            options={[
              {
                value: ansatt.ident ?? "",
                label: `${navn} - ${ansatt?.ident}`,
              },
            ]}
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
            {redigeringsModus ? "Lagre redigert avtale" : "Registrer avtale"}{" "}
          </Button>
        </div>
      </form>
    </FormProvider>
  );
}

export const FormGroup = ({
  children,
  cols = 1,
}: {
  children: ReactNode;
  cols?: number;
}) => (
  <div
    className={classNames(styles.form_group, styles.grid, {
      [styles.grid_1]: cols === 1,
      [styles.grid_2]: cols === 2,
    })}
  >
    {children}
  </div>
);
