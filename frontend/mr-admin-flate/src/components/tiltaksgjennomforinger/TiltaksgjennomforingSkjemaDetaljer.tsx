import { PlusIcon, XMarkIcon } from "@navikt/aksel-icons";
import {
  Alert,
  Button,
  Checkbox,
  HStack,
  HelpText,
  Select,
  Switch,
  TextField,
} from "@navikt/ds-react";
import { Avtale, Tiltaksgjennomforing, Toggles } from "mulighetsrommet-api-client";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common";
import { useEffect, useRef } from "react";
import { useFieldArray, useFormContext } from "react-hook-form";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useHentKontaktpersoner } from "../../api/ansatt/useHentKontaktpersoner";
import { useTiltaksgjennomforingAdministratorer } from "../../api/ansatt/useTiltaksgjennomforingAdministratorer";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { useVirksomhet } from "../../api/virksomhet/useVirksomhet";
import { useVirksomhetKontaktpersoner } from "../../api/virksomhet/useVirksomhetKontaktpersoner";
import { addYear } from "../../utils/Utils";
import { isTiltakMedFellesOppstart } from "../../utils/tiltakskoder";
import { Separator } from "../detaljside/Metadata";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FormGroup } from "../skjema/FormGroup";
import { FraTilDatoVelger } from "../skjema/FraTilDatoVelger";
import skjemastyles from "../skjema/Skjema.module.scss";
import { VirksomhetKontaktpersonerModal } from "../virksomhet/VirksomhetKontaktpersonerModal";
import { SelectOppstartstype } from "./SelectOppstartstype";
import {
  arrangorUnderenheterOptions,
  erArenaOpphavOgIngenEierskap,
} from "./TiltaksgjennomforingSkjemaConst";
import { InferredTiltaksgjennomforingSchema } from "./TiltaksgjennomforingSchema";
import { useMigrerteTiltakstyper } from "../../api/tiltakstyper/useMigrerteTiltakstyper";

interface Props {
  tiltaksgjennomforing?: Tiltaksgjennomforing;
  avtale: Avtale;
}

export const TiltaksgjennomforingSkjemaDetaljer = ({ tiltaksgjennomforing, avtale }: Props) => {
  const { data: virksomhet } = useVirksomhet(avtale.leverandor.organisasjonsnummer);
  const { data: administratorer } = useTiltaksgjennomforingAdministratorer();
  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();
  const { data: kontaktpersoner, isLoading: isLoadingKontaktpersoner } = useHentKontaktpersoner();
  const { data: migrerteTiltakstyper = [] } = useMigrerteTiltakstyper();

  const virksomhetKontaktpersonerModalRef = useRef<HTMLDialogElement>(null);

  const kontaktpersonerOption = (selectedIndex: number) => {
    const excludedKontaktpersoner = watch("kontaktpersoner")
      ?.filter((_: any, i: number) => i !== selectedIndex)
      .map((k: any) => k["navIdent"]);

    const options = kontaktpersoner
      ?.filter((kontaktperson) => !excludedKontaktpersoner?.includes(kontaktperson.navIdent))
      ?.map((kontaktperson) => ({
        label: `${kontaktperson.fornavn} ${kontaktperson.etternavn} - ${kontaktperson.navIdent}`,
        value: kontaktperson.navIdent,
      }));

    return options || [];
  };

  const {
    register,
    control,
    formState: { errors },
    setValue,
    watch,
  } = useFormContext<InferredTiltaksgjennomforingSchema>();
  const {
    fields: kontaktpersonFields,
    append: appendKontaktperson,
    remove: removeKontaktperson,
  } = useFieldArray({
    name: "kontaktpersoner",
    control,
  });
  const {
    data: virksomhetKontaktpersoner,
    isLoading: isLoadingVirksomhetKontaktpersoner,
    refetch: refetchVirksomhetKontaktpersoner,
  } = useVirksomhetKontaktpersoner(avtale.leverandor.organisasjonsnummer);

  const watchErMidlertidigStengt = watch("midlertidigStengt.erMidlertidigStengt");
  const watchVisEstimertVentetid = watch("visEstimertVentetid");

  useEffect(() => {
    const resetEstimertVentetid = () => {
      if (!watchVisEstimertVentetid) {
        setValue("estimertVentetid", null);
      }
    };

    resetEstimertVentetid();
  }, [watchVisEstimertVentetid]);

  const regionerOptions = avtale.kontorstruktur
    .map((struk) => struk.region)
    .map((kontor) => ({ value: kontor.enhetsnummer, label: kontor.navn }));

  const navEnheterOptions = avtale.kontorstruktur
    .flatMap((struk) => struk.kontorer)
    .filter((kontor) => kontor.overordnetEnhet === watch("navRegion"))
    .map((kontor) => ({ label: kontor.navn, value: kontor.enhetsnummer }));

  const minStartdato = new Date();
  const maxSluttdato = addYear(minStartdato, 5);
  const { data: midlertidigStengt } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_MIDLERTIDIG_STENGT,
  );

  const valgteNavEnheter = watch("navEnheter");

  return (
    <div className={skjemastyles.container}>
      <div className={skjemastyles.input_container}>
        <div className={skjemastyles.column}>
          <FormGroup>
            <TextField
              size="small"
              readOnly={erArenaOpphavOgIngenEierskap(tiltaksgjennomforing, migrerteTiltakstyper)}
              error={errors.navn?.message as string}
              label="Tiltaksnavn"
              autoFocus
              {...register("navn")}
            />
            {tiltaksgjennomforing?.tiltaksnummer ? (
              <TextField
                size="small"
                readOnly
                label="Tiltaksnummer"
                autoFocus
                value={tiltaksgjennomforing?.tiltaksnummer}
              />
            ) : null}
          </FormGroup>
          <Separator />
          <FormGroup>
            <TextField
              size="small"
              readOnly
              label={`Avtale (tiltakstype: ${avtale.tiltakstype.navn})`}
              value={avtale.navn || ""}
            />
            {errors.avtaleId?.message ? (
              <Alert variant="warning">{errors.avtaleId.message as string}</Alert>
            ) : null}
          </FormGroup>
          <Separator />
          <FormGroup>
            <SelectOppstartstype name="oppstart" />
            <FraTilDatoVelger
              size="small"
              fra={{
                label: "Startdato",
                readOnly: erArenaOpphavOgIngenEierskap(tiltaksgjennomforing, migrerteTiltakstyper),
                fromDate: minStartdato,
                toDate: maxSluttdato,
                ...register("startOgSluttDato.startDato"),
                format: "iso-string",
              }}
              til={{
                label: "Sluttdato",
                readOnly: erArenaOpphavOgIngenEierskap(tiltaksgjennomforing, migrerteTiltakstyper),
                fromDate: minStartdato,
                toDate: maxSluttdato,
                ...register("startOgSluttDato.sluttDato"),
                format: "iso-string",
              }}
            />
            <Checkbox
              size="small"
              readOnly={erArenaOpphavOgIngenEierskap(tiltaksgjennomforing, migrerteTiltakstyper)}
              {...register("apentForInnsok")}
            >
              Åpen for innsøk
            </Checkbox>
            {midlertidigStengt ? (
              <Checkbox size="small" {...register("midlertidigStengt.erMidlertidigStengt")}>
                Midlertidig stengt
              </Checkbox>
            ) : null}
            {watchErMidlertidigStengt && (
              <FraTilDatoVelger
                size="small"
                fra={{
                  label: "Stengt fra",
                  fromDate: minStartdato,
                  toDate: maxSluttdato,
                  ...register("midlertidigStengt.stengtFra"),
                  format: "date",
                }}
                til={{
                  label: "Stengt til",
                  fromDate: watch("midlertidigStengt.stengtFra") ?? new Date(),
                  toDate: maxSluttdato,
                  ...register("midlertidigStengt.stengtTil"),
                  format: "date",
                }}
              />
            )}
            <HStack justify="space-between" columns={2}>
              <TextField
                size="small"
                readOnly={erArenaOpphavOgIngenEierskap(tiltaksgjennomforing, migrerteTiltakstyper)}
                error={errors.antallPlasser?.message as string}
                type="number"
                style={{ width: "180px" }}
                label="Antall plasser"
                {...register("antallPlasser", {
                  valueAsNumber: true,
                })}
              />
              {isTiltakMedFellesOppstart(avtale.tiltakstype.arenaKode) && (
                <TextField
                  size="small"
                  readOnly={erArenaOpphavOgIngenEierskap(
                    tiltaksgjennomforing,
                    migrerteTiltakstyper,
                  )}
                  error={errors.deltidsprosent?.message as string}
                  type="number"
                  step="0.01"
                  min={0}
                  max={100}
                  style={{ width: "180px" }}
                  label="Deltidsprosent"
                  {...register("deltidsprosent", {
                    valueAsNumber: true,
                  })}
                />
              )}
            </HStack>
            <Separator />
            <fieldset className={skjemastyles.fieldset_no_styling}>
              <HStack gap="1">
                <legend>Estimert ventetid </legend>
                <HelpText title="Hva er estimert ventetid?">
                  Estimert ventetid er et felt som kan brukes hvis dere sitter på informasjon om
                  estimert ventetid for tiltaket. Hvis dere legger inn en verdi i feltene her blir
                  det synlig for alle ansatte i NAV.
                </HelpText>
              </HStack>
              <Switch checked={watch("visEstimertVentetid")} {...register("visEstimertVentetid")}>
                Registrer estimert ventetid
              </Switch>
              {watch("visEstimertVentetid") ? (
                <HStack justify="start" gap="10" columns={4}>
                  <TextField
                    size="small"
                    type="number"
                    min={0}
                    label="Antall"
                    error={errors.estimertVentetid?.verdi?.message as string}
                    {...register("estimertVentetid.verdi", {
                      valueAsNumber: true,
                    })}
                  />
                  <Select
                    size="small"
                    label="Måleenhet"
                    error={errors.estimertVentetid?.enhet?.message as string}
                    {...register("estimertVentetid.enhet")}
                  >
                    <option value="uke">Uker</option>
                    <option value="maned">Måneder</option>
                  </Select>
                </HStack>
              ) : null}
            </fieldset>
          </FormGroup>
          <Separator />
          <FormGroup>
            <ControlledMultiSelect
              size="small"
              placeholder={isLoadingAnsatt ? "Laster..." : "Velg en"}
              label={"Administratorer for gjennomføringen"}
              helpText="Bestemmer hvem som eier gjennomføringen. Notifikasjoner sendes til administratorene."
              {...register("administratorer")}
              options={AdministratorOptions(
                ansatt,
                tiltaksgjennomforing?.administratorer,
                administratorer,
              )}
            />
          </FormGroup>
        </div>
        <div className={skjemastyles.vertical_separator} />
        <div className={skjemastyles.column}>
          <div className={skjemastyles.gray_container}>
            <FormGroup>
              <ControlledSokeSelect
                size="small"
                label="NAV-region"
                placeholder="Velg en"
                {...register("navRegion")}
                onChange={() => {
                  setValue("navEnheter", [] as any);
                }}
                options={regionerOptions}
              />
              <ControlledMultiSelect
                size="small"
                placeholder={"Velg en"}
                label={"NAV-enheter (kontorer)"}
                helpText="Bestemmer hvem gjennomføringen skal vises til i Modia, basert på hvilket kontor brukeren har tilhørighet til."
                {...register("navEnheter")}
                options={navEnheterOptions}
              />
            </FormGroup>
            <Separator />
            <FormGroup>
              <div>
                {kontaktpersonFields?.map((field, index) => {
                  return (
                    <div className={skjemastyles.kontaktperson_container} key={field.id}>
                      <Button
                        className={skjemastyles.kontaktperson_fjern_button}
                        variant="tertiary"
                        size="small"
                        type="button"
                        onClick={() => {
                          if (watch("kontaktpersoner")!.length > 1) {
                            removeKontaktperson(index);
                          } else {
                            setValue("kontaktpersoner", [
                              {
                                navIdent: "",
                                navEnheter: [],
                              },
                            ]);
                          }
                        }}
                      >
                        <XMarkIcon fontSize="1.5rem" />
                      </Button>
                      <div className={skjemastyles.kontaktperson_inputs}>
                        <ControlledSokeSelect
                          helpText="Bestemmer kontaktperson som veilederene kan hendvende seg til for informasjon om gjennomføringen. Kan gjelde for én eller flere enheter."
                          size="small"
                          placeholder={
                            isLoadingKontaktpersoner ? "Laster kontaktpersoner..." : "Velg en"
                          }
                          label={"Kontaktperson i NAV"}
                          {...register(`kontaktpersoner.${index}.navIdent`, {
                            shouldUnregister: true,
                          })}
                          options={kontaktpersonerOption(index)}
                        />
                        <ControlledMultiSelect
                          size="small"
                          placeholder={isLoadingKontaktpersoner ? "Laster enheter..." : "Velg en"}
                          label={"Område"}
                          {...register(`kontaktpersoner.${index}.navEnheter`, {
                            shouldUnregister: true,
                          })}
                          options={navEnheterOptions.filter((enhet) =>
                            valgteNavEnheter.includes(enhet.value),
                          )}
                        />
                        <TextField
                          size="small"
                          label="Beskrivelse"
                          placeholder="Unngå personopplysninger"
                          maxLength={67}
                          {...register(`kontaktpersoner.${index}.beskrivelse`, {
                            shouldUnregister: true,
                          })}
                        />
                      </div>
                    </div>
                  );
                })}
                <Button
                  className={skjemastyles.kontaktperson_button}
                  type="button"
                  size="small"
                  variant="tertiary"
                  onClick={() =>
                    appendKontaktperson({
                      navIdent: "",
                      navEnheter: [],
                    })
                  }
                >
                  <PlusIcon aria-label="Legg til ny kontaktperson" /> Legg til ny kontaktperson
                </Button>
              </div>
            </FormGroup>
          </div>
          <div className={skjemastyles.gray_container}>
            <FormGroup>
              <TextField
                size="small"
                label="Tiltaksarrangør hovedenhet"
                placeholder=""
                defaultValue={`${avtale.leverandor.navn} - ${avtale.leverandor.organisasjonsnummer}`}
                readOnly
              />
              <ControlledSokeSelect
                size="small"
                label="Tiltaksarrangør underenhet"
                placeholder="Velg underenhet for tiltaksarrangør"
                {...register("tiltaksArrangorUnderenhetOrganisasjonsnummer")}
                onClearValue={() => {
                  setValue("tiltaksArrangorUnderenhetOrganisasjonsnummer", "");
                }}
                readOnly={
                  !avtale.leverandor.organisasjonsnummer ||
                  erArenaOpphavOgIngenEierskap(tiltaksgjennomforing, migrerteTiltakstyper)
                }
                options={arrangorUnderenheterOptions(avtale, virksomhet)}
              />
              <div className={skjemastyles.virksomhet_kontaktperson_container}>
                <ControlledMultiSelect
                  size="small"
                  placeholder={
                    isLoadingVirksomhetKontaktpersoner ? "Laster kontaktpersoner..." : "Velg en"
                  }
                  label={"Kontaktperson hos arrangøren"}
                  {...register("arrangorKontaktpersoner")}
                  options={
                    virksomhetKontaktpersoner?.map((person) => ({
                      value: person.id,
                      label: person.navn,
                    })) ?? []
                  }
                />
                <Button
                  className={skjemastyles.kontaktperson_button}
                  size="small"
                  type="button"
                  variant="tertiary"
                  onClick={() => virksomhetKontaktpersonerModalRef.current?.showModal()}
                >
                  Rediger eller legg til kontaktpersoner
                </Button>
              </div>
              <TextField
                size="small"
                label="Sted for gjennomføring"
                description="Skriv inn stedet tiltaket skal gjennomføres, for eksempel Fredrikstad eller Tromsø. For tiltak uten eksplisitt lokasjon (for eksempel digital jobbklubb), kan du la feltet stå tomt."
                {...register("stedForGjennomforing")}
                error={
                  errors.stedForGjennomforing
                    ? (errors.stedForGjennomforing.message as string)
                    : null
                }
              />
            </FormGroup>
          </div>
        </div>
        <VirksomhetKontaktpersonerModal
          orgnr={avtale.leverandor.organisasjonsnummer}
          modalRef={virksomhetKontaktpersonerModalRef}
          onClose={() => {
            refetchVirksomhetKontaktpersoner().then((res) => {
              setValue(
                "arrangorKontaktpersoner",
                watch("arrangorKontaktpersoner").filter((id: string) =>
                  res?.data?.some((p) => p.id === id),
                ),
              );
            });
          }}
        />
      </div>
    </div>
  );
};
