import { faro } from "@grafana/faro-web-sdk";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button, Textarea, TextField } from "@navikt/ds-react";
import classNames from "classnames";
import {
  ApiError,
  Avtale,
  AvtaleRequest,
  Avtaletype,
  NavAnsatt,
  Norg2Type,
  Opphav,
  Utkast,
} from "mulighetsrommet-api-client";
import { NavEnhet } from "mulighetsrommet-api-client/build/models/NavEnhet";
import { Tiltakstype } from "mulighetsrommet-api-client/build/models/Tiltakstype";
import { StatusModal } from "mulighetsrommet-veileder-flate/src/components/modal/delemodal/StatusModal";
import { ReactNode, useEffect, useRef, useState } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { toast } from "react-toastify";
import { v4 as uuidv4 } from "uuid";
import { useHentBetabrukere } from "../../api/ansatt/useHentBetabrukere";
import { usePutAvtale } from "../../api/avtaler/usePutAvtale";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";
import { useSokVirksomheter } from "../../api/virksomhet/useSokVirksomhet";
import { useVirksomhet } from "../../api/virksomhet/useVirksomhet";
import {
  formaterDatoSomYYYYMMDD,
  tiltakstypekodeErAnskaffetTiltak,
} from "../../utils/Utils";
import { AutoSaveUtkast } from "../autosave/AutoSaveUtkast";
import { Separator } from "../detaljside/Metadata";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FraTilDatoVelger } from "../skjema/FraTilDatoVelger";
import { SelectOption, SokeSelect } from "../skjema/SokeSelect";
import { VirksomhetKontaktpersoner } from "../virksomhet/VirksomhetKontaktpersoner";
import { AvtaleSchema, inferredSchema } from "./AvtaleSchema";
import styles from "./OpprettAvtaleContainer.module.scss";

type UtkastData = Pick<
  Avtale,
  | "navn"
  | "tiltakstype"
  | "navRegion"
  | "navEnheter"
  | "ansvarlig"
  | "avtaletype"
  | "leverandor"
  | "leverandorUnderenheter"
  | "leverandorKontaktperson"
  | "startDato"
  | "sluttDato"
  | "url"
  | "prisbetingelser"
> & {
  avtaleId: string;
  id: string;
};

interface OpprettAvtaleContainerProps {
  onClose: () => void;
  onSuccess: (id: string) => void;
  tiltakstyper: Tiltakstype[];
  ansatt: NavAnsatt;
  avtale?: Avtale;
  enheter: NavEnhet[];
}

export function OpprettAvtaleContainer({
  onClose,
  onSuccess,
  tiltakstyper,
  ansatt,
  enheter,
  avtale,
}: OpprettAvtaleContainerProps) {
  const mutation = usePutAvtale();
  const redigeringsModus = !!avtale;
  const [navRegion, setNavRegion] = useState<string | undefined>(
    avtale?.navRegion?.enhetsnummer
  );
  const [sokLeverandor, setSokLeverandor] = useState(
    avtale?.leverandor.organisasjonsnummer || ""
  );
  const { data: leverandorVirksomheter = [] } =
    useSokVirksomheter(sokLeverandor);
  const { data: betabrukere } = useHentBetabrukere();
  const { data: features } = useFeatureToggles();
  const mutationUtkast = useMutateUtkast();
  const utkastIdRef = useRef(avtale?.id || uuidv4());

  const getTiltakstypeFromId = (id: string): Tiltakstype | undefined => {
    return tiltakstyper.find((type) => type.id === id);
  };

  const saveUtkast = (values: inferredSchema) => {
    const utkastData: UtkastData = {
      navn: values?.avtalenavn,
      tiltakstype: {
        id: values?.tiltakstype,
        arenaKode: "",
        navn: "",
      },
      navRegion: {
        navn: "",
        enhetsnummer: values?.navRegion,
      },
      navEnheter: values?.navEnheter?.map((enhetsnummer) => ({
        navn: "",
        enhetsnummer,
      })),
      ansvarlig: { navident: values?.avtaleansvarlig },
      avtaletype: values?.avtaletype,
      leverandor: {
        navn: "",
        organisasjonsnummer: values?.leverandor,
      },
      leverandorUnderenheter: values?.leverandorUnderenheter?.map(
        (organisasjonsnummer) => ({ navn: "", organisasjonsnummer })
      ),
      startDato: values?.startOgSluttDato?.startDato?.toDateString(),
      sluttDato: values?.startOgSluttDato?.sluttDato?.toDateString(),
      url: values?.url,
      prisbetingelser: values?.prisOgBetalingsinfo || "",
      avtaleId: avtale?.id || utkastIdRef.current,
      id: avtale?.id || utkastIdRef.current,
    };

    if (!values.avtalenavn) {
      toast.info("For å lagre utkast må du gi utkastet et navn", {
        autoClose: 10000,
      });
      return;
    }

    mutationUtkast.mutate({
      id: utkastIdRef.current,
      utkastData,
      type: Utkast.type.AVTALE,
      opprettetAv: ansatt?.navIdent,
      avtaleId: utkastIdRef.current,
    });
  };

  const defaultEnhet = () => {
    if (avtale?.navRegion?.enhetsnummer) {
      return avtale?.navRegion?.enhetsnummer;
    }
    if (
      enheter.find((e) => e.enhetsnummer === ansatt.hovedenhet.enhetsnummer)
    ) {
      return ansatt.hovedenhet.enhetsnummer;
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
        avtale?.navEnheter?.length === 0
          ? ["alle_enheter"]
          : avtale?.navEnheter?.map((e) => e.enhetsnummer),
      avtaleansvarlig: avtale?.ansvarlig?.navident || ansatt.navIdent || "",
      avtalenavn: getValueOrDefault(avtale?.navn, ""),
      avtaletype: getValueOrDefault(avtale?.avtaletype, Avtaletype.AVTALE),
      leverandor: getValueOrDefault(
        avtale?.leverandor?.organisasjonsnummer,
        ""
      ),
      leverandorUnderenheter:
        avtale?.leverandorUnderenheter?.length === 0
          ? []
          : avtale?.leverandorUnderenheter?.map(
              (enhet) => enhet.organisasjonsnummer
            ),
      leverandorKontaktpersonId: avtale?.leverandorKontaktperson?.id,
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
    formState: { errors, defaultValues },
    watch,
    setValue,
  } = form;

  const watchedTiltakstype = watch("tiltakstype");

  useEffect(() => {
    const arenaKode = getTiltakstypeFromId(watchedTiltakstype)?.arenaKode || "";
    if (["ARBFORB", "VASV"].includes(arenaKode)) {
      setValue("avtaletype", Avtaletype.FORHAANDSGODKJENT);
    }
  }, [watchedTiltakstype]);

  const { data: leverandorData } = useVirksomhet(watch("leverandor"));
  const underenheterForLeverandor = getValueOrDefault(
    leverandorData?.underenheter,
    []
  );

  const erAnskaffetTiltak = (tiltakstypeId: string): boolean => {
    const tiltakstype = getTiltakstypeFromId(tiltakstypeId);
    return tiltakstypekodeErAnskaffetTiltak(tiltakstype?.arenaKode);
  };

  const arenaOpphav = avtale?.opphav === Opphav.ARENA;

  const postData: SubmitHandler<inferredSchema> = async (
    data
  ): Promise<void> => {
    const {
      navRegion,
      navEnheter,
      leverandor: leverandorOrganisasjonsnummer,
      leverandorUnderenheter,
      leverandorKontaktpersonId,
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
      leverandorKontaktpersonId,
    };

    if (avtale?.id) {
      requestBody.id = avtale.id; // Ved oppdatering av eksisterende avtale
    }

    mutation.mutate(requestBody);
  };

  if (mutation.isSuccess) {
    onSuccess(mutation.data.id);
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
        onClose={onClose}
        primaryButtonOnClick={() => mutation.reset()}
        primaryButtonText="Prøv igjen"
        secondaryButtonOnClick={onClose}
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
        <div className={styles.container}>
          <Separator />
          <div className={styles.input_container}>
            <div className={styles.column}>
              <FormGroup>
                <TextField
                  size="small"
                  readOnly={arenaOpphav}
                  error={errors.avtalenavn?.message}
                  label="Avtalenavn"
                  autoFocus
                  data-testid="avtalenavn-input"
                  {...register("avtalenavn")}
                />
              </FormGroup>
              <Separator />
              <FormGroup cols={2}>
                <SokeSelect
                  size="small"
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
                  size="small"
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
              <Separator />
              <FormGroup>
                <FraTilDatoVelger
                  size="small"
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
              <Separator />
              <FormGroup>
                <TextField
                  size="small"
                  error={errors.url?.message}
                  label="URL til avtale"
                  {...register("url")}
                />
              </FormGroup>
              <Separator />
              {erAnskaffetTiltak(watch("tiltakstype")) ? (
                <>
                  <FormGroup>
                    <Textarea
                      size="small"
                      readOnly={arenaOpphav}
                      error={errors.prisOgBetalingsinfo?.message}
                      label="Pris og betalingsinformasjon"
                      {...register("prisOgBetalingsinfo")}
                    />
                  </FormGroup>
                  <Separator />
                </>
              ) : null}
              <FormGroup>
                <SokeSelect
                  size="small"
                  placeholder="Velg en"
                  label={"Avtaleansvarlig"}
                  {...register("avtaleansvarlig")}
                  onClearValue={() => setValue("avtaleansvarlig", "")}
                  options={ansvarligOptions(
                    ansatt,
                    avtale?.ansvarlig,
                    betabrukere
                  )}
                />
              </FormGroup>
            </div>
            <div className={styles.vertical_separator} />
            <div className={styles.column}>
              <div className={styles.gray_container}>
                <FormGroup>
                  <SokeSelect
                    size="small"
                    placeholder="Velg en"
                    label={"NAV region"}
                    {...register("navRegion")}
                    onChange={(e) => {
                      setNavRegion(e);
                      form.setValue("navEnheter", [] as any);
                    }}
                    onClearValue={() => setValue("navRegion", "")}
                    options={enheter
                      .filter((enhet) => enhet.type === Norg2Type.FYLKE)
                      .map((enhet) => ({
                        value: `${enhet.enhetsnummer}`,
                        label: enhet.navn,
                      }))}
                  />
                  <ControlledMultiSelect
                    size="small"
                    placeholder="Velg en"
                    readOnly={!navRegion}
                    label={"NAV enhet (kontorer)"}
                    {...register("navEnheter")}
                    options={enheterOptions()}
                  />
                </FormGroup>
              </div>
              <div className={styles.gray_container}>
                <FormGroup>
                  <SokeSelect
                    size="small"
                    readOnly={arenaOpphav}
                    placeholder="Søk etter tiltaksarrangør"
                    label={"Tiltaksarrangør hovedenhet"}
                    {...register("leverandor")}
                    onInputChange={(value) => setSokLeverandor(value)}
                    onClearValue={() => setValue("leverandor", "")}
                    options={leverandorVirksomheter.map((enhet) => ({
                      value: enhet.organisasjonsnummer,
                      label: `${enhet.navn} - ${enhet.organisasjonsnummer}`,
                    }))}
                  />
                  <ControlledMultiSelect
                    size="small"
                    placeholder="Velg underenhet for tiltaksarrangør"
                    label={"Tiltaksarrangør underenhet"}
                    readOnly={!watch("leverandor")}
                    {...register("leverandorUnderenheter")}
                    options={underenheterOptions()}
                  />
                </FormGroup>
                {watch("leverandor") && (
                  <FormGroup>
                    <div className={styles.kontaktperson_container}>
                      <VirksomhetKontaktpersoner
                        title={"Kontaktperson hos leverandøren"}
                        orgnr={watch("leverandor")}
                        formValueName={"leverandorKontaktpersonId"}
                      />
                    </div>
                  </FormGroup>
                )}
              </div>
            </div>
          </div>
          <Separator />
          <div className={styles.button_row}>
            <Button
              className={styles.button}
              onClick={onClose}
              variant="tertiary"
              type="button"
            >
              Avbryt
            </Button>
            <Button
              className={styles.button}
              type="submit"
              onClick={() => {
                faro?.api?.pushEvent(
                  `Bruker ${
                    redigeringsModus ? "redigerer" : "oppretter"
                  } avtale`,
                  { handling: redigeringsModus ? "redigerer" : "oppretter" },
                  "avtale"
                );
              }}
            >
              {redigeringsModus ? "Lagre redigert avtale" : "Registrer avtale"}
            </Button>
          </div>
        </div>
      </form>
      {features?.["mulighetsrommet.admin-flate-lagre-utkast"] ? (
        <AutoSaveUtkast
          defaultValues={defaultValues}
          utkastId={utkastIdRef.current}
          onSave={() => saveUtkast(watch())}
          mutation={mutationUtkast}
        />
      ) : null}
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
  <div className={styles.form_group}>
    <div
      className={classNames(styles.grid, {
        [styles.grid_1]: cols === 1,
        [styles.grid_2]: cols === 2,
      })}
    >
      {children}
    </div>
  </div>
);

export const ansvarligOptions = (
  ansatt?: NavAnsatt,
  ansvarlig?: { navident?: string; navn?: string },
  betabrukere?: NavAnsatt[]
): SelectOption[] => {
  if (!ansatt || !betabrukere) {
    return [{ value: "", label: "Laster..." }];
  }

  const options = [
    {
      value: ansatt.navIdent ?? "",
      label: `${ansatt.fornavn} ${ansatt?.etternavn} - ${ansatt?.navIdent}`,
    },
  ];

  if (ansvarlig?.navident && ansvarlig.navident !== ansatt?.navIdent) {
    options.push({
      value: ansvarlig.navident,
      label: `${ansvarlig.navn} - ${ansvarlig.navident}`,
    });
  }

  betabrukere
    .filter(
      (b: NavAnsatt) =>
        b.navIdent !== ansatt.navIdent && b.navIdent !== ansvarlig?.navident
    )
    .forEach((b: NavAnsatt) =>
      options.push({
        value: b.navIdent,
        label: `${b.fornavn} ${b.etternavn} - ${b.navIdent}`,
      })
    );

  return options;
};
