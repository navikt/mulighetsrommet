import { Alert, Button, DatePicker, Textarea, TextField, useDatepicker } from "@navikt/ds-react";
import {
  Avtale,
  AvtaleAvslutningsstatus,
  AvtaleRequest,
  Avtalestatus,
  Avtaletype,
  EmbeddedTiltakstype,
  LeverandorUnderenhet,
  NavAnsatt,
  NavEnhetType,
  Opphav,
  Tiltakskode,
  Toggles,
} from "mulighetsrommet-api-client";
import { NavEnhet } from "mulighetsrommet-api-client/build/models/NavEnhet";
import { Tiltakstype } from "mulighetsrommet-api-client/build/models/Tiltakstype";
import { useEffect, useRef, useState } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import { useHentBetabrukere } from "../../api/ansatt/useHentBetabrukere";
import { usePutAvtale } from "../../api/avtaler/usePutAvtale";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";
import { useSokVirksomheter } from "../../api/virksomhet/useSokVirksomhet";
import { useVirksomhet } from "../../api/virksomhet/useVirksomhet";
import { addYear, formaterDato, formaterDatoSomYYYYMMDD } from "../../utils/Utils";
import { AutoSaveUtkast } from "../autosave/AutoSaveUtkast";
import { Separator } from "../detaljside/Metadata";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FraTilDatoVelger } from "../skjema/FraTilDatoVelger";
import skjemastyles from "../skjema/Skjema.module.scss";
import { SokeSelect } from "../skjema/SokeSelect";
import { VirksomhetKontaktpersoner } from "../virksomhet/VirksomhetKontaktpersoner";
import { AvtaleSchema, InferredAvtaleSchema } from "./AvtaleSchema";

import { zodResolver } from "@hookform/resolvers/zod";
import { PORTEN } from "mulighetsrommet-frontend-common/constants";
import { resolveErrorMessage } from "../../api/errors";
import { erAnskaffetTiltak } from "../../utils/tiltakskoder";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { FormGroup } from "../skjema/FormGroup";
import {
  defaultEnhet,
  getLokaleUnderenheterAsSelectOptions,
  saveUtkast,
  underenheterOptions,
} from "./AvtaleSkjemaConst";
import { AvtaleSkjemaKnapperad } from "./AvtaleSkjemaKnapperad";
import { AvbrytAvtaleModal } from "../modal/AvbrytAvtaleModal";
import { useFeatureToggle } from "../../api/features/feature-toggles";

interface Props {
  onClose: () => void;
  onSuccess: (id: string) => void;
  tiltakstyper: Tiltakstype[];
  ansatt: NavAnsatt;
  avtale?: Avtale;
  enheter: NavEnhet[];
  redigeringsModus: boolean;
}

export function AvtaleSkjemaContainer({
  onClose,
  onSuccess,
  tiltakstyper,
  ansatt,
  enheter,
  avtale,
  redigeringsModus,
}: Props) {
  const [navRegion, setNavRegion] = useState<string | undefined>(avtale?.navRegion?.enhetsnummer);
  const [sokLeverandor, setSokLeverandor] = useState(avtale?.leverandor?.organisasjonsnummer || "");
  const avbrytModalRef = useRef<HTMLDialogElement>(null);

  const { data: enableOpsjoner } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_OPSJONER_FOR_AVTALER,
  );
  const mutation = usePutAvtale();
  const { data: betabrukere } = useHentBetabrukere();
  const mutationUtkast = useMutateUtkast();

  const { data: leverandorVirksomheter = [] } = useSokVirksomheter(sokLeverandor);

  const utkastIdRef = useRef(avtale?.id || uuidv4());

  const form = useForm<InferredAvtaleSchema>({
    resolver: zodResolver(AvtaleSchema),
    defaultValues: {
      tiltakstype: avtale?.tiltakstype,
      navRegion: defaultEnhet(avtale, enheter, ansatt),
      navEnheter: avtale?.navEnheter?.map((e) => e.enhetsnummer) || [],
      administrator: avtale?.administrator?.navIdent || ansatt.navIdent || "",
      avtalenavn: avtale?.navn ?? "",
      avtaletype: avtale?.avtaletype ?? Avtaletype.AVTALE,
      leverandor: avtale?.leverandor?.organisasjonsnummer ?? "",
      leverandorUnderenheter:
        avtale?.leverandorUnderenheter?.length === 0 || !avtale?.leverandorUnderenheter
          ? []
          : avtale?.leverandorUnderenheter?.map(
              (leverandor: LeverandorUnderenhet) => leverandor.organisasjonsnummer,
            ),
      leverandorKontaktpersonId: avtale?.leverandorKontaktperson?.id,
      startOgSluttDato: {
        startDato: avtale?.startDato ? new Date(avtale.startDato) : undefined,
        sluttDato: avtale?.sluttDato ? new Date(avtale.sluttDato) : undefined,
      },
      url: avtale?.url ?? undefined,
      prisOgBetalingsinfo: avtale?.prisbetingelser ?? undefined,
      opphav: avtale?.opphav ?? Opphav.MR_ADMIN_FLATE,
    },
  });

  const {
    register,
    handleSubmit,
    formState: { errors, defaultValues },
    watch,
    setValue,
  } = form;

  const {
    datepickerProps: maksVarighetDatepickerProps,
    inputProps: maksVarighetDatepickerInputProps,
  } = useDatepicker({
    fromDate: new Date(),
    defaultSelected:
      defaultValues?.startOgSluttDato?.sluttDato &&
      addYear(defaultValues?.startOgSluttDato?.sluttDato, 5),
  });

  const watchedTiltakstype: EmbeddedTiltakstype | undefined = watch("tiltakstype");
  const arenaKode = watchedTiltakstype?.arenaKode;

  useEffect(() => {
    // TODO: revurdere behovet for denne type logikk eller om det kan defineres som default felter på tiltakstype i stedet
    // Er det slik at tiltakstype alltid styrer avtaletypen? Er det kun for forhåndsgodkjente avtaler?
    // Hvis ARBFORB og VASV uansett alltid skal være av typen FORHAANDSGODKJENT burde det ikke være mulig å endre
    if (arenaKode === Tiltakskode.ARBFORB || arenaKode === Tiltakskode.VASV) {
      setValue("avtaletype", Avtaletype.FORHAANDSGODKJENT);
    }
  }, [arenaKode]);

  const watchedLeverandor = watch("leverandor");
  const { data: leverandorData } = useVirksomhet(watchedLeverandor);

  const underenheterForLeverandor = leverandorData?.underenheter ?? [];

  const arenaOpphav = avtale?.opphav === Opphav.ARENA;

  const postData: SubmitHandler<InferredAvtaleSchema> = async (data): Promise<void> => {
    const {
      navRegion,
      navEnheter,
      leverandor: leverandorOrganisasjonsnummer,
      leverandorUnderenheter,
      leverandorKontaktpersonId,
      avtalenavn: navn,
      startOgSluttDato,
      tiltakstype,
      administrator,
      avtaletype,
      prisOgBetalingsinfo,
      opphav,
      url,
    } = data;

    const requestBody: AvtaleRequest = {
      id: utkastIdRef.current,
      navRegion,
      navEnheter,
      avtalenummer: avtale?.avtalenummer || null,
      leverandorOrganisasjonsnummer,
      leverandorUnderenheter,
      navn,
      sluttDato: formaterDatoSomYYYYMMDD(startOgSluttDato.sluttDato),
      startDato: formaterDatoSomYYYYMMDD(startOgSluttDato.startDato),
      tiltakstypeId: tiltakstype.id,
      url: url || null,
      administrator,
      avtaletype,
      prisOgBetalingsinformasjon: erAnskaffetTiltak(tiltakstype.arenaKode)
        ? prisOgBetalingsinfo || null
        : null,
      opphav,
      leverandorKontaktpersonId: leverandorKontaktpersonId ?? null,
      avslutningsstatus: AvtaleAvslutningsstatus.IKKE_AVSLUTTET,
    };

    if (avtale?.id) {
      requestBody.id = avtale.id; // Ved oppdatering av eksisterende avtale
    }

    mutation.mutate(requestBody);
  };

  useEffect(() => {
    if (mutation.isSuccess) {
      onSuccess(mutation.data.id);
    }
  }, [mutation]);

  if (mutation.isError) {
    return (
      <Alert variant="error">
        {mutation.error.status === 400 ? (
          resolveErrorMessage(mutation.error)
        ) : (
          <>
            Avtalen kunne ikke opprettes på grunn av en teknisk feil hos oss. Forsøk på nytt eller
            ta <a href={PORTEN}>kontakt i Porten</a> dersom du trenger mer hjelp.
          </>
        )}
      </Alert>
    );
  }

  const navRegionerOptions = enheter
    .filter((enhet) => enhet.type === NavEnhetType.FYLKE)
    .map((enhet) => ({
      value: enhet.enhetsnummer,
      label: enhet.navn,
    }));

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <div className={skjemastyles.container}>
          <AvtaleSkjemaKnapperad redigeringsModus={redigeringsModus!} onClose={onClose} />
          <Separator />
          <div className={skjemastyles.input_container}>
            <div className={skjemastyles.column}>
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
                    value: {
                      arenaKode: tiltakstype.arenaKode,
                      navn: tiltakstype.navn,
                      id: tiltakstype.id,
                    },
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
                >
                  {enableOpsjoner &&
                  watch("avtaletype") === Avtaletype.RAMMEAVTALE &&
                  !!watch("startOgSluttDato.sluttDato") ? (
                    <DatePicker {...maksVarighetDatepickerProps}>
                      <DatePicker.Input
                        {...maksVarighetDatepickerInputProps}
                        label="Maks varighet inkl. opsjon"
                        readOnly
                        size="small"
                        value={formaterDato(addYear(watch("startOgSluttDato.sluttDato"), 5))}
                      />
                    </DatePicker>
                  ) : null}
                </FraTilDatoVelger>
              </FormGroup>
              <Separator />
              <FormGroup>
                <TextField
                  size="small"
                  error={errors.url?.message}
                  label="URL til avtale fra Mercell eller Websak"
                  {...register("url")}
                />
              </FormGroup>
              <Separator />
              {arenaKode && erAnskaffetTiltak(arenaKode) && (
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
              )}
              <FormGroup>
                <SokeSelect
                  size="small"
                  placeholder="Velg en"
                  label={"Administrator for avtalen"}
                  {...register("administrator")}
                  onClearValue={() => setValue("administrator", "")}
                  description="Den som blir satt som administrator vil få en notifikasjon."
                  options={AdministratorOptions(ansatt, avtale?.administrator, betabrukere)}
                />
              </FormGroup>
            </div>
            <div className={skjemastyles.vertical_separator} />
            <div className={skjemastyles.column}>
              <div className={skjemastyles.gray_container}>
                <FormGroup>
                  <SokeSelect
                    size="small"
                    placeholder="Velg en"
                    label={"NAV-region"}
                    {...register("navRegion")}
                    onChange={(e) => {
                      setNavRegion(e.target.value);
                      form.setValue("navEnheter", [] as any);
                    }}
                    onClearValue={() => setValue("navRegion", "")}
                    options={navRegionerOptions}
                  />
                  <ControlledMultiSelect
                    size="small"
                    placeholder="Velg en"
                    readOnly={!navRegion}
                    label={"NAV-enheter (kontorer)"}
                    {...register("navEnheter")}
                    options={getLokaleUnderenheterAsSelectOptions(navRegion, enheter)}
                  />
                </FormGroup>
              </div>
              <div className={skjemastyles.gray_container}>
                <FormGroup>
                  <SokeSelect
                    size="small"
                    readOnly={arenaOpphav}
                    placeholder="Søk etter tiltaksarrangør"
                    label={"Tiltaksarrangør hovedenhet"}
                    {...register("leverandor")}
                    onInputChange={(value) => {
                      // Beholder søket hvis input settes til "" for å sørge for at listen med options
                      // ikke forsvinner når man velger en leverandør
                      if (value) {
                        setSokLeverandor(value);
                      }
                    }}
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
                    readOnly={!watchedLeverandor}
                    {...register("leverandorUnderenheter")}
                    options={underenheterOptions(underenheterForLeverandor)}
                  />
                </FormGroup>
                {watchedLeverandor && !avtale?.leverandor?.slettet && (
                  <FormGroup>
                    <div className={skjemastyles.kontaktperson_container}>
                      <VirksomhetKontaktpersoner
                        title="Kontaktperson hos leverandøren"
                        orgnr={watchedLeverandor}
                        formValueName="leverandorKontaktpersonId"
                      />
                    </div>
                  </FormGroup>
                )}
              </div>
            </div>
          </div>
          <Separator />
          <div>
            {avtale && !arenaOpphav && avtale.avtalestatus === Avtalestatus.AKTIV && (
              <Button
                size="small"
                variant="danger"
                type="button"
                onClick={() => avbrytModalRef.current?.showModal()}
                data-testid="avbryt-avtale"
              >
                Avbryt avtale
              </Button>
            )}
          </div>
        </div>
      </form>
      <AutoSaveUtkast
        defaultValues={defaultValues}
        utkastId={utkastIdRef.current}
        onSave={() => saveUtkast(watch(), avtale!, ansatt, utkastIdRef, mutationUtkast)}
        mutation={mutationUtkast}
      />
      {avtale && <AvbrytAvtaleModal modalRef={avbrytModalRef} avtale={avtale} />}
    </FormProvider>
  );
}
