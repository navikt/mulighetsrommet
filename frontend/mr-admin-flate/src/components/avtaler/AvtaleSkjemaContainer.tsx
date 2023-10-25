import { Button, DatePicker, Textarea, TextField, useDatepicker } from "@navikt/ds-react";
import {
  Avtale,
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
import { useUpsertAvtale } from "../../api/avtaler/useUpsertAvtale";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";
import { useSokVirksomheter } from "../../api/virksomhet/useSokVirksomhet";
import { useVirksomhet } from "../../api/virksomhet/useVirksomhet";
import { addYear, formaterDato, formaterDatoTid } from "../../utils/Utils";
import { Separator } from "../detaljside/Metadata";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FraTilDatoVelger } from "../skjema/FraTilDatoVelger";
import skjemastyles from "../skjema/Skjema.module.scss";
import { SelectOption, SokeSelect } from "../skjema/SokeSelect";
import { VirksomhetKontaktpersoner } from "../virksomhet/VirksomhetKontaktpersoner";
import { AvtaleSchema, InferredAvtaleSchema } from "./AvtaleSchema";

import { zodResolver } from "@hookform/resolvers/zod";
import { MultiValue } from "react-select";
import { useHandleApiUpsertResponse } from "../../api/effects";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { erAnskaffetTiltak } from "../../utils/tiltakskoder";
import { AvbrytAvtaleModal } from "../modal/AvbrytAvtaleModal";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { FormGroup } from "../skjema/FormGroup";
import {
  AvtaleUtkastData,
  getLokaleUnderenheterAsSelectOptions,
  saveUtkast,
  underenheterOptions,
} from "./AvtaleSkjemaConst";
import { AvtaleSkjemaKnapperad } from "./AvtaleSkjemaKnapperad";

interface Props {
  onClose: () => void;
  onSuccess: (id: string) => void;
  tiltakstyper: Tiltakstype[];
  ansatt: NavAnsatt;
  avtale?: Avtale;
  avtaleUtkast?: AvtaleUtkastData;
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
  avtaleUtkast,
  redigeringsModus,
}: Props) {
  const [sokLeverandor, setSokLeverandor] = useState(
    avtaleUtkast?.leverandor ?? (avtale?.leverandor?.organisasjonsnummer || ""),
  );
  const avbrytModalRef = useRef<HTMLDialogElement>(null);

  const { data: enableOpsjoner } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_OPSJONER_FOR_AVTALER,
  );
  const mutation = useUpsertAvtale();
  const { data: betabrukere } = useHentBetabrukere();
  const mutationUtkast = useMutateUtkast();

  const { data: leverandorVirksomheter = [] } = useSokVirksomheter(sokLeverandor);

  const utkastIdRef = useRef(avtaleUtkast?.id || avtale?.id || uuidv4());

  const navRegioner =
    avtaleUtkast?.navRegioner ??
    avtale?.kontorstruktur.map((struktur) => struktur.region.enhetsnummer) ??
    [];
  const navEnheter =
    avtaleUtkast?.navEnheter ??
    avtale?.kontorstruktur
      .flatMap((struktur) => struktur.kontorer)
      .map((enhet) => enhet.enhetsnummer) ??
    [];

  const form = useForm<InferredAvtaleSchema>({
    resolver: zodResolver(AvtaleSchema),
    defaultValues: {
      tiltakstype: avtaleUtkast?.tiltakstype ?? avtale?.tiltakstype,
      navRegioner,
      navEnheter,
      administrator:
        avtaleUtkast?.administrator ?? (avtale?.administrator?.navIdent || ansatt.navIdent || ""),
      navn: avtaleUtkast?.navn ?? avtale?.navn ?? "",
      avtaletype: avtaleUtkast?.avtaletype ?? avtale?.avtaletype ?? Avtaletype.AVTALE,
      leverandor: avtaleUtkast?.leverandor ?? avtale?.leverandor?.organisasjonsnummer ?? "",
      leverandorUnderenheter:
        avtaleUtkast?.leverandorUnderenheter ??
        (avtale?.leverandorUnderenheter?.length === 0 || !avtale?.leverandorUnderenheter)
          ? []
          : avtale?.leverandorUnderenheter?.map(
              (leverandor: LeverandorUnderenhet) => leverandor.organisasjonsnummer,
            ),
      leverandorKontaktpersonId:
        avtaleUtkast?.leverandorKontaktpersonId ?? avtale?.leverandorKontaktperson?.id,
      startOgSluttDato: avtaleUtkast?.startOgSluttDato ?? {
        startDato: avtale?.startDato ? avtale.startDato : undefined,
        sluttDato: avtale?.sluttDato ? avtale.sluttDato : undefined,
      },
      url: avtaleUtkast?.url ?? avtale?.url ?? undefined,
      prisbetingelser: avtaleUtkast?.prisbetingelser ?? avtale?.prisbetingelser ?? undefined,
      opphav: avtaleUtkast?.opphav ?? avtale?.opphav ?? Opphav.MR_ADMIN_FLATE,
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
      (defaultValues?.startOgSluttDato?.sluttDato &&
        addYear(new Date(defaultValues?.startOgSluttDato?.sluttDato), 5)) ||
      undefined,
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

  const defaultUpdatedAt = avtale?.updatedAt;
  const [lagreState, setLagreState] = useState(
    defaultUpdatedAt ? `Sist lagret: ${formaterDatoTid(defaultUpdatedAt)}` : undefined,
  );

  const postData: SubmitHandler<InferredAvtaleSchema> = async (data): Promise<void> => {
    const requestBody: AvtaleRequest = {
      id: avtale?.id ?? utkastIdRef.current,
      navEnheter: data.navEnheter.concat(data.navRegioner),
      avtalenummer: avtale?.avtalenummer || null,
      leverandorOrganisasjonsnummer: data.leverandor,
      leverandorUnderenheter: data.leverandorUnderenheter,
      navn: data.navn,
      sluttDato: data.startOgSluttDato.sluttDato,
      startDato: data.startOgSluttDato.startDato,
      tiltakstypeId: data.tiltakstype.id,
      url: data.url || null,
      administrator: data.administrator,
      avtaletype: data.avtaletype,
      prisbetingelser: erAnskaffetTiltak(data.tiltakstype.arenaKode)
        ? data.prisbetingelser || null
        : null,
      opphav: data.opphav,
      leverandorKontaktpersonId: data.leverandorKontaktpersonId ?? null,
    };

    mutation.mutate(requestBody);
  };

  useHandleApiUpsertResponse(
    mutation,
    (response) => onSuccess(response.id),
    (validation) => {
      validation.errors.forEach((error) => {
        const name = mapErrorToSchemaPropertyName(error.name);
        form.setError(name, { type: "custom", message: error.message });
      });

      function mapErrorToSchemaPropertyName(name: string) {
        const mapping: { [name: string]: string } = {
          startDato: "startOgSluttDato.startDato",
          sluttDato: "startOgSluttDato.sluttDato",
          leverandorOrganisasjonsnummer: "leverandor",
          tiltakstypeId: "tiltakstype",
        };
        return (mapping[name] ?? name) as keyof InferredAvtaleSchema;
      }
    },
  );

  const navRegionerOptions = enheter
    .filter((enhet) => enhet.type === NavEnhetType.FYLKE)
    .map((enhet) => ({
      value: enhet.enhetsnummer,
      label: enhet.navn,
    }));

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <AvtaleSkjemaKnapperad
          redigeringsModus={redigeringsModus!}
          onClose={onClose}
          defaultValues={defaultValues}
          utkastIdRef={utkastIdRef.current}
          saveUtkast={() =>
            saveUtkast(watch(), avtale!, ansatt, utkastIdRef, mutationUtkast, setLagreState)
          }
          mutationUtkast={mutationUtkast}
          lagreState={lagreState}
          setLagreState={setLagreState}
        />
        <Separator classname={skjemastyles.avtaleskjema_separator} />
        <div className={skjemastyles.container}>
          <div className={skjemastyles.input_container}>
            <div className={skjemastyles.column}>
              <FormGroup>
                <TextField
                  size="small"
                  readOnly={arenaOpphav}
                  error={errors.navn?.message}
                  label="Avtalenavn"
                  autoFocus
                  data-testid="avtalenavn-input"
                  {...register("navn")}
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
                    format: "iso-string",
                  }}
                  til={{
                    readOnly: arenaOpphav,
                    ...register("startOgSluttDato.sluttDato"),
                    label: "Sluttdato",
                    format: "iso-string",
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
                        value={formaterDato(
                          addYear(new Date(watch("startOgSluttDato.sluttDato")), 5),
                        )}
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
                      error={errors.prisbetingelser?.message}
                      label="Pris og betalingsinformasjon"
                      {...register("prisbetingelser")}
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
                  <ControlledMultiSelect
                    size="small"
                    placeholder="Velg en"
                    label={"NAV-regioner"}
                    {...register("navRegioner")}
                    additionalOnChange={(selectedOptions) => {
                      if (watch("navRegioner").length > 1) {
                        const alleLokaleUnderenheter = velgAlleLokaleUnderenheter(
                          selectedOptions,
                          enheter,
                        );
                        form.setValue(
                          "navEnheter",
                          alleLokaleUnderenheter as [string, ...string[]],
                        );
                      } else {
                        const alleLokaleUnderenheter = velgAlleLokaleUnderenheter(
                          selectedOptions,
                          enheter,
                        );
                        const navEnheter = watch("navEnheter").filter((enhet) =>
                          alleLokaleUnderenheter.includes(enhet),
                        );
                        form.setValue("navEnheter", navEnheter as [string, ...string[]]);
                      }
                    }}
                    options={navRegionerOptions}
                  />
                  <ControlledMultiSelect
                    size="small"
                    placeholder="Velg en"
                    label={"NAV-enheter (kontorer)"}
                    {...register("navEnheter")}
                    options={getLokaleUnderenheterAsSelectOptions(watch("navRegioner"), enheter)}
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
      {avtale && <AvbrytAvtaleModal modalRef={avbrytModalRef} avtale={avtale} />}
    </FormProvider>
  );
}

function velgAlleLokaleUnderenheter(
  selectedOptions: MultiValue<SelectOption<string>>,
  enheter: NavEnhet[],
): string[] {
  const regioner = selectedOptions?.map((option) => option.value);
  const navEnheter = getLokaleUnderenheterAsSelectOptions(regioner, enheter).map(
    (option) => option.value,
  );
  return navEnheter;
}
