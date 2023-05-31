import { zodResolver } from "@hookform/resolvers/zod";
import { Button, Textarea, TextField } from "@navikt/ds-react";
import classNames from "classnames";
import {
  Avtale,
  AvtaleRequest,
  Avtaletype,
  Norg2Type,
} from "mulighetsrommet-api-client";
import { Ansatt } from "mulighetsrommet-api-client/build/models/Ansatt";
import { NavEnhet } from "mulighetsrommet-api-client/build/models/NavEnhet";
import { Tiltakstype } from "mulighetsrommet-api-client/build/models/Tiltakstype";
import { porten } from "mulighetsrommet-frontend-common/constants";
import { StatusModal } from "mulighetsrommet-veileder-flate/src/components/modal/delemodal/StatusModal";
import { Dispatch, ReactNode, SetStateAction, useState } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { mulighetsrommetClient } from "../../api/clients";
import { useSokVirksomheter } from "../../api/virksomhet/useSokVirksomhet";
import { useVirksomhet } from "../../api/virksomhet/useVirksomhet";
import { useNavigerTilAvtale } from "../../hooks/useNavigerTilAvtale";
import {
  capitalize,
  formaterDatoSomYYYYMMDD,
  tiltakstypekodeErAnskaffetTiltak,
} from "../../utils/Utils";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { SokeSelect } from "../skjema/SokeSelect";
import styles from "./OpprettAvtaleContainer.module.scss";
import { Datovelger } from "../skjema/Datovelger";
import { AvtaleSchema, inferredSchema } from "./AvtaleSchema";
import { useFeatureToggles } from "../../api/features/feature-toggles";

interface OpprettAvtaleContainerProps {
  onAvbryt: () => void;
  setResult: Dispatch<SetStateAction<string | null>>;
  tiltakstyper: Tiltakstype[];
  ansatt: Ansatt;
  avtale?: Avtale;
  enheter: NavEnhet[];
}

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
  const [navRegion, setNavRegion] = useState<string | undefined>(
    avtale?.navRegion?.enhetsnummer
  );
  const [sokLeverandor, setSokLeverandor] = useState(
    avtale?.leverandor.organisasjonsnummer || ""
  );
  const { data: leverandorVirksomheter = [] } =
    useSokVirksomheter(sokLeverandor);
  const { data: features } = useFeatureToggles();

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

  function getValueOrDefault<T>(
    value: T | undefined | null,
    defaultValue: T
  ): T {
    return value || defaultValue;
  }

  const form = useForm<inferredSchema>({
    resolver: zodResolver(AvtaleSchema),
    defaultValues: {
      tiltakstype: avtale?.tiltakstype?.id,
      navRegion: defaultEnhet(),
      navEnheter:
        avtale?.navEnheter.length === 0
          ? ["alle_enheter"]
          : avtale?.navEnheter.map((e) => e.enhetsnummer),
      avtaleansvarlig: avtale?.ansvarlig || ansatt?.ident || "",
      avtalenavn: getValueOrDefault(avtale?.navn, ""),
      avtaletype: getValueOrDefault(avtale?.avtaletype, Avtaletype.AVTALE),
      leverandor: getValueOrDefault(
        avtale?.leverandor?.organisasjonsnummer,
        ""
      ),
      leverandorUnderenheter:
        avtale?.leverandorUnderenheter.length === 0
          ? []
          : avtale?.leverandorUnderenheter?.map(
              (enhet) => enhet.organisasjonsnummer
            ),
      antallPlasser: getValueOrDefault(avtale?.antallPlasser, 0),
      startDato: avtale?.startDato ? new Date(avtale.startDato) : null,
      sluttDato: avtale?.sluttDato ? new Date(avtale.sluttDato) : null,
      url: getValueOrDefault(avtale?.url, ""),
      prisOgBetalingsinfo: getValueOrDefault(
        avtale?.prisbetingelser,
        undefined
      ),
    },
  });
  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
  } = form;

  const { data: leverandorData } = useVirksomhet(watch("leverandor"));
  const underenheterForLeverandor = getValueOrDefault(
    leverandorData?.underenheter,
    []
  );

  const erAnskaffetTiltak = (tiltakstypeId: string): boolean => {
    const tiltakstype = tiltakstyper.find((type) => type.id === tiltakstypeId);
    return tiltakstypekodeErAnskaffetTiltak(tiltakstype?.arenaKode);
  };

  const postData: SubmitHandler<inferredSchema> = async (
    data
  ): Promise<void> => {
    setFeil(null);
    setResult(null);

    if (!features?.["mulighetsrommet.admin-flate-lagre-data-fra-admin-flate"]) {
      alert(
        "Opprettelse av avtale er ikke skrudd på enda. Kontakt Team Valp ved spørsmål."
      );
      return;
    }

    const {
      antallPlasser,
      navRegion,
      navEnheter,
      leverandor: leverandorOrganisasjonsnummer,
      leverandorUnderenheter,
      avtalenavn: navn,
      sluttDato,
      startDato,
      tiltakstype: tiltakstypeId,
      avtaleansvarlig: ansvarlig,
      avtaletype,
      prisOgBetalingsinfo,
      url,
    } = data;

    const requestBody: AvtaleRequest = {
      antallPlasser,
      navRegion,
      navEnheter: navEnheter.includes("alle_enheter") ? [] : navEnheter,
      avtalenummer: getValueOrDefault(avtale?.avtalenummer, ""),
      leverandorOrganisasjonsnummer,
      leverandorUnderenheter: leverandorUnderenheter.includes(
        "alle_underenheter"
      )
        ? []
        : leverandorUnderenheter,
      navn,
      sluttDato: formaterDatoSomYYYYMMDD(sluttDato),
      startDato: formaterDatoSomYYYYMMDD(startDato),
      tiltakstypeId,
      url,
      ansvarlig,
      avtaletype,
      prisOgBetalingsinformasjon: erAnskaffetTiltak(tiltakstypeId)
        ? prisOgBetalingsinfo
        : undefined,
    };

    if (avtale?.id) {
      requestBody.id = avtale.id; // Ved oppdatering av eksisterende avtale
    }

    try {
      const response = await mulighetsrommetClient.avtaler.opprettAvtale({
        requestBody,
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
        return navRegion === enhet.overordnetEnhet;
      })
      .map((enhet: NavEnhet) => ({
        label: enhet.navn,
        value: enhet.enhetsnummer,
      }));
    options?.unshift({ value: "alle_enheter", label: "Alle enheter" });
    return options || [];
  };

  const underenheterOptions = () => {
    const options = underenheterForLeverandor.map((enhet) => ({
      value: enhet.organisasjonsnummer,
      label: `${enhet.navn} - ${enhet.organisasjonsnummer}`,
    }));

    options?.unshift({
      value: "alle_underenheter",
      label: "Alle underenheter",
    });
    return options;
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
              ...register("startDato"),
              label: "Startdato",
              error: errors.startDato?.message,
            }}
            til={{
              ...register("sluttDato"),
              label: "Sluttdato",
              error: errors.sluttDato?.message,
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
        <FormGroup cols={1}>
          <SokeSelect
            placeholder="Søk etter tiltaksarrangør"
            label={"Tiltaksarrangør hovedenhet"}
            {...register("leverandor")}
            onInputChange={(value) => setSokLeverandor(value)}
            options={leverandorVirksomheter.map((enhet) => ({
              value: enhet.organisasjonsnummer,
              label: `${enhet.navn} - ${enhet.organisasjonsnummer}`,
            }))}
          />
          <ControlledMultiSelect
            placeholder="Velg underenhet for tiltaksarrangør"
            label={"Tiltaksarrangør underenhet"}
            disabled={!watch("leverandor")}
            {...register("leverandorUnderenheter")}
            options={underenheterOptions()}
          />
        </FormGroup>
        <FormGroup>
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
