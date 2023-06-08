import { zodResolver } from "@hookform/resolvers/zod";
import { Button, Textarea, TextField } from "@navikt/ds-react";
import classNames from "classnames";
import {
  ApiError,
  Avtale,
  AvtaleRequest,
  Avtaletype,
  Norg2Type,
  Opphav,
} from "mulighetsrommet-api-client";
import { Ansatt } from "mulighetsrommet-api-client/build/models/Ansatt";
import { NavEnhet } from "mulighetsrommet-api-client/build/models/NavEnhet";
import { Tiltakstype } from "mulighetsrommet-api-client/build/models/Tiltakstype";
import { StatusModal } from "mulighetsrommet-veileder-flate/src/components/modal/delemodal/StatusModal";
import { ReactNode, useState } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { usePutAvtale } from "../../api/avtaler/usePutAvtale";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { useSokVirksomheter } from "../../api/virksomhet/useSokVirksomhet";
import { useVirksomhet } from "../../api/virksomhet/useVirksomhet";
import { useNavigerTilAvtale } from "../../hooks/useNavigerTilAvtale";
import { arenaKodeErAftEllerVta } from "../../utils/tiltakskoder";
import {
  capitalize,
  formaterDatoSomYYYYMMDD,
  tiltakstypekodeErAnskaffetTiltak,
} from "../../utils/Utils";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FraTilDatoVelger } from "../skjema/FraTilDatoVelger";
import { SokeSelect } from "../skjema/SokeSelect";
import { AvtaleSchema, inferredSchema } from "./AvtaleSchema";
import styles from "./OpprettAvtaleContainer.module.scss";
import { faro } from "@grafana/faro-web-sdk";

interface OpprettAvtaleContainerProps {
  onAvbryt: () => void;
  tiltakstyper: Tiltakstype[];
  ansatt: Ansatt;
  avtale?: Avtale;
  enheter: NavEnhet[];
}

export function OpprettAvtaleContainer({
  onAvbryt,
  tiltakstyper,
  ansatt,
  enheter,
  avtale,
}: OpprettAvtaleContainerProps) {
  const mutation = usePutAvtale();
  const { navigerTilAvtale } = useNavigerTilAvtale();
  const redigeringsModus = !!avtale;
  const [navRegion, setNavRegion] = useState<string | undefined>(
    avtale?.navRegion?.enhetsnummer
  );
  const [sokLeverandor, setSokLeverandor] = useState(
    avtale?.leverandor.organisasjonsnummer || ""
  );
  const { data: leverandorVirksomheter = [] } =
    useSokVirksomheter(sokLeverandor);
  const { data: features } = useFeatureToggles();

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
      startOgSluttDato: {
        startDato: avtale?.startDato ? new Date(avtale.startDato) : undefined,
        sluttDato: avtale?.sluttDato ? new Date(avtale.sluttDato) : undefined,
      },
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

  const arenaOpphav = avtale?.opphav === Opphav.ARENA;
  const navn = ansatt?.fornavn
    ? [ansatt.fornavn, ansatt.etternavn ?? ""]
        .map((it) => capitalize(it))
        .join(" ")
    : "";

  const postData: SubmitHandler<inferredSchema> = async (
    data
  ): Promise<void> => {
    const arenaKodeForTiltakstype = tiltakstyper.find(
      (type) => type.id === data.tiltakstype
    )?.arenaKode;

    const avtaleErVtaEllerAft = arenaKodeErAftEllerVta(arenaKodeForTiltakstype);

    const enableAvtale = avtaleErVtaEllerAft
      ? true
      : features?.["mulighetsrommet.admin-flate-lagre-data-fra-admin-flate"];

    if (!enableAvtale) {
      alert(
        "Opprettelse av avtale er ikke skrudd på enda. Kontakt Team Valp ved spørsmål."
      );
      return;
    }

    const {
      navRegion,
      navEnheter,
      leverandor: leverandorOrganisasjonsnummer,
      leverandorUnderenheter,
      avtalenavn: navn,
      startOgSluttDato,
      tiltakstype: tiltakstypeId,
      avtaleansvarlig: ansvarlig,
      avtaletype,
      prisOgBetalingsinfo,
      url,
    } = data;

    const requestBody: AvtaleRequest = {
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
      sluttDato: formaterDatoSomYYYYMMDD(startOgSluttDato.sluttDato),
      startDato: formaterDatoSomYYYYMMDD(startOgSluttDato.startDato),
      tiltakstypeId,
      url,
      ansvarlig,
      avtaletype,
      prisOgBetalingsinformasjon: erAnskaffetTiltak(tiltakstypeId)
        ? prisOgBetalingsinfo
        : undefined,
      opphav: avtale?.opphav,
    };

    if (avtale?.id) {
      requestBody.id = avtale.id; // Ved oppdatering av eksisterende avtale
    }

    mutation.mutate(requestBody);
  };

  if (mutation.isSuccess) {
    navigerTilAvtale(mutation.data.id);
  }

  if (mutation.isError) {
    return (
      <StatusModal
        modalOpen={!!mutation.isError}
        ikonVariant="error"
        heading="Kunne ikke opprette avtale"
        text={
          <>
            {(mutation.error as ApiError).status === 400
              ? (mutation.error as ApiError).body
              : "Avtalen kunne ikke opprettes på grunn av en teknisk feil hos oss. " +
                "Forsøk på nytt eller ta <a href={porten}>kontakt</a> i Porten dersom " +
                "du trenger mer hjelp."}
          </>
        }
        onClose={onAvbryt}
        primaryButtonOnClick={() => mutation.reset()}
        primaryButtonText="Prøv igjen"
        secondaryButtonOnClick={onAvbryt}
        secondaryButtonText="Avbryt"
      />
    );
  }

  const ansvarligOptions = () => {
    const options = [];
    if (avtale?.ansvarlig && avtale.ansvarlig !== ansatt?.ident) {
      options.push({
        value: avtale?.ansvarlig,
        label: avtale?.ansvarlig,
      });
    }

    options.push({
      value: ansatt?.ident ?? "",
      label: `${navn} - ${ansatt?.ident}`,
    });

    return options;
  };

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
            readOnly={arenaOpphav}
            error={errors.avtalenavn?.message}
            label="Avtalenavn"
            {...register("avtalenavn")}
          />
        </FormGroup>
        <FormGroup cols={2}>
          <SokeSelect
            readOnly={arenaOpphav}
            placeholder="Velg en"
            label={"Tiltakstype"}
            {...register("tiltakstype")}
            options={tiltakstyper.map((tiltakstype) => ({
              value: tiltakstype.id,
              label: tiltakstype.navn,
            }))}
          />
          <SokeSelect
            readOnly={arenaOpphav}
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
        </FormGroup>
        <FormGroup>
          <FraTilDatoVelger
            fra={{
              readOnly: arenaOpphav,
              ...register("startOgSluttDato.startDato"),
              label: "Startdato",
            }}
            til={{
              readOnly: arenaOpphav,
              ...register("startOgSluttDato.sluttDato"),
              label: "Sluttdato",
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
            readOnly={!navRegion}
            label={"NAV enhet (kontorer)"}
            {...register("navEnheter")}
            options={enheterOptions()}
          />
        </FormGroup>
        <FormGroup cols={1}>
          <SokeSelect
            readOnly={arenaOpphav}
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
            readOnly={!watch("leverandor")}
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
              readOnly={arenaOpphav}
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
            options={ansvarligOptions()}
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
          <Button
            className={styles.button}
            type="submit"
            onClick={() => {
              faro.api.pushEvent(
                "Bruker lagrer avtale",
                { handling: redigeringsModus ? "redigerer" : "oppretter" },
                "avtale"
              );
            }}
          >
            {redigeringsModus ? "Lagre redigert avtale" : "Registrer avtale"}
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
