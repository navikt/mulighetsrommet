import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useHentKontaktpersoner } from "@/api/ansatt/useHentKontaktpersoner";
import { useTiltaksgjennomforingAdministratorer } from "@/api/ansatt/useTiltaksgjennomforingAdministratorer";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { useTiltaksgjennomforingDeltakerSummary } from "@/api/tiltaksgjennomforing/useTiltaksgjennomforingDeltakerSummary";
import { useMigrerteTiltakstyper } from "@/api/tiltakstyper/useMigrerteTiltakstyper";
import { addYear, formaterDato } from "@/utils/Utils";
import { isTiltakMedFellesOppstart } from "@/utils/tiltakskoder";
import { PlusIcon, XMarkIcon } from "@navikt/aksel-icons";
import {
  Alert,
  Button,
  DatePicker,
  HGrid,
  HStack,
  HelpText,
  Select,
  Switch,
  TextField,
  UNSAFE_Combobox,
} from "@navikt/ds-react";
import {
  Avtale,
  Tiltaksgjennomforing,
  TiltaksgjennomforingKontaktperson,
  TiltaksgjennomforingOppstartstype,
  TiltakskodeArena,
  Toggles,
} from "mulighetsrommet-api-client";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common";
import { useEffect, useRef } from "react";
import { useFieldArray, useFormContext } from "react-hook-form";
import { mockNusData } from "../../mocks/fixtures/mock_nusData";
import { Separator } from "../detaljside/Metadata";
import { tiltaktekster } from "../ledetekster/tiltaksgjennomforingLedetekster";
import { EndreDatoAdvarselModal } from "../modal/EndreDatoAdvarselModal";
import { InferredTiltaksgjennomforingSchema } from "../redaksjonelt-innhold/TiltaksgjennomforingSchema";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { ControlledDateInput } from "../skjema/ControlledDateInput";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FormGroup } from "../skjema/FormGroup";
import skjemastyles from "../skjema/Skjema.module.scss";
import { SelectOppstartstype } from "./SelectOppstartstype";
import { TiltakTilgjengeligForArrangor } from "./TilgjengeligTiltakForArrangor";
import { TiltaksgjennomforingArrangorSkjema } from "./TiltaksgjennomforingArrangorSkjema";
import { erArenaOpphavOgIngenEierskap } from "./TiltaksgjennomforingSkjemaConst";

interface Props {
  tiltaksgjennomforing?: Tiltaksgjennomforing;
  avtale: Avtale;
}

function visApentForInnsok(arenaKode: TiltakskodeArena) {
  return [
    TiltakskodeArena.JOBBK,
    TiltakskodeArena.DIGIOPPARB,
    TiltakskodeArena.GRUPPEAMO,
    TiltakskodeArena.GRUFAGYRKE,
  ].includes(arenaKode);
}

export const TiltaksgjennomforingSkjemaDetaljer = ({ tiltaksgjennomforing, avtale }: Props) => {
  const { data: administratorer } = useTiltaksgjennomforingAdministratorer();
  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();
  const { data: kontaktpersoner, isLoading: isLoadingKontaktpersoner } = useHentKontaktpersoner();
  const { data: migrerteTiltakstyper = [] } = useMigrerteTiltakstyper();
  const { data: deltakerSummary } = useTiltaksgjennomforingDeltakerSummary(
    tiltaksgjennomforing?.id,
  );
  const { data: enableTilgjengeligForArrangor } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_TILGJENGELIGGJORE_TILTAK_FOR_ARRANGOR,
  );
  const endreStartDatoModalRef = useRef<HTMLDialogElement>(null);
  const endreSluttDatoModalRef = useRef<HTMLDialogElement>(null);

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

  const watchVisEstimertVentetid = watch("visEstimertVentetid");

  useEffect(() => {
    const resetEstimertVentetid = () => {
      if (!watchVisEstimertVentetid) {
        setValue("estimertVentetid", null);
      }
    };

    resetEstimertVentetid();
  }, [watchVisEstimertVentetid]);

  const watchStartDato = watch("startOgSluttDato.startDato");
  useEffect(() => {
    if (
      tiltaksgjennomforing &&
      deltakerSummary?.antallDeltakere &&
      deltakerSummary.antallDeltakere > 0 &&
      tiltaksgjennomforing.startDato !== watchStartDato
    ) {
      endreStartDatoModalRef.current?.showModal();
    }
  }, [watchStartDato]);

  const watchSluttDato = watch("startOgSluttDato.sluttDato");
  useEffect(() => {
    if (
      tiltaksgjennomforing &&
      deltakerSummary?.antallDeltakere &&
      deltakerSummary.antallDeltakere > 0 &&
      tiltaksgjennomforing.sluttDato !== watchSluttDato
    ) {
      endreSluttDatoModalRef.current?.showModal();
    }
  }, [watchSluttDato]);

  const regionerOptions = avtale.kontorstruktur
    .map((struk) => struk.region)
    .map((kontor) => ({ value: kontor.enhetsnummer, label: kontor.navn }));

  const navEnheterOptions = avtale.kontorstruktur
    .flatMap((struk) => struk.kontorer)
    .filter((kontor) => kontor.overordnetEnhet === watch("navRegion"))
    .map((kontor) => ({ label: kontor.navn, value: kontor.enhetsnummer }));

  const minStartdato = new Date(avtale.startDato);
  const maxSluttdato = addYear(minStartdato, 5);

  const valgteNavEnheter = watch("navEnheter");

  const eierIkkeGjennomforing = erArenaOpphavOgIngenEierskap(
    tiltaksgjennomforing,
    migrerteTiltakstyper,
  );

  return (
    <div className={skjemastyles.container}>
      <div className={skjemastyles.input_container}>
        <div className={skjemastyles.column}>
          <FormGroup>
            <TextField
              size="small"
              readOnly={eierIkkeGjennomforing}
              error={errors.navn?.message as string}
              label={tiltaktekster.tiltaksnavnLabel}
              autoFocus
              {...register("navn")}
            />
            {tiltaksgjennomforing?.tiltaksnummer ? (
              <TextField
                size="small"
                readOnly
                label={tiltaktekster.tiltaksnummerLabel}
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
              label={tiltaktekster.avtaleMedTiltakstype(avtale.tiltakstype.navn)}
              value={avtale.navn || ""}
            />
            {errors.avtaleId?.message ? (
              <Alert variant="warning">{errors.avtaleId.message as string}</Alert>
            ) : null}
            {avtale.tiltakstype.arenaKode === TiltakskodeArena.GRUFAGYRKE ? (
              <VelgUtdanningskategori />
            ) : null}
          </FormGroup>
          <Separator />
          <FormGroup>
            <SelectOppstartstype
              name="oppstart"
              readonly={!isTiltakMedFellesOppstart(avtale.tiltakstype.arenaKode)}
            />
            <HGrid columns={2}>
              <DatePicker>
                <DatePicker.Input
                  value={formaterDato(avtale.startDato)}
                  label={tiltaktekster.avtaleStartdatoLabel}
                  readOnly
                  size="small"
                />
              </DatePicker>
              {avtale.sluttDato ? (
                <DatePicker>
                  <DatePicker.Input
                    value={formaterDato(avtale.sluttDato)}
                    label={tiltaktekster.avtaleSluttdatoLabel}
                    readOnly
                    size="small"
                  />
                </DatePicker>
              ) : (
                " - "
              )}
            </HGrid>
            <HGrid columns={2}>
              <ControlledDateInput
                size="small"
                label={tiltaktekster.startdatoLabel}
                readOnly={eierIkkeGjennomforing}
                fromDate={minStartdato}
                toDate={maxSluttdato}
                {...register("startOgSluttDato.startDato")}
                format={"iso-string"}
              />
              <ControlledDateInput
                size="small"
                label={tiltaktekster.sluttdatoLabel}
                readOnly={eierIkkeGjennomforing}
                fromDate={minStartdato}
                toDate={maxSluttdato}
                {...register("startOgSluttDato.sluttDato")}
                format={"iso-string"}
              />
            </HGrid>
            {visApentForInnsok(avtale.tiltakstype.arenaKode) ? (
              <Switch
                size="small"
                readOnly={eierIkkeGjennomforing}
                {...register("apentForInnsok")}
                checked={watch("apentForInnsok")}
              >
                {tiltaktekster.apentForInnsokLabel}
              </Switch>
            ) : null}
            <HGrid align="start" columns={2}>
              <TextField
                size="small"
                readOnly={eierIkkeGjennomforing}
                error={errors.antallPlasser?.message as string}
                type="number"
                style={{ width: "180px" }}
                label={tiltaktekster.antallPlasserLabel}
                {...register("antallPlasser", {
                  valueAsNumber: true,
                })}
              />
              {isTiltakMedFellesOppstart(avtale.tiltakstype.arenaKode) && (
                <TextField
                  size="small"
                  readOnly={eierIkkeGjennomforing}
                  error={errors.deltidsprosent?.message as string}
                  type="number"
                  step="0.01"
                  min={0}
                  max={100}
                  style={{ width: "180px" }}
                  label={tiltaktekster.deltidsprosentLabel}
                  {...register("deltidsprosent", {
                    valueAsNumber: true,
                  })}
                />
              )}
            </HGrid>
            {watch("oppstart") === TiltaksgjennomforingOppstartstype.LOPENDE ? (
              <>
                <Separator />
                <fieldset className={skjemastyles.fieldset_no_styling}>
                  <HStack gap="1">
                    <legend>Estimert ventetid</legend>
                    <HelpText title="Hva er estimert ventetid?">
                      Estimert ventetid er et felt som kan brukes hvis dere sitter på informasjon om
                      estimert ventetid for tiltaket. Hvis dere legger inn en verdi i feltene her
                      blir det synlig for alle ansatte i NAV.
                    </HelpText>
                  </HStack>
                  <Switch
                    checked={watch("visEstimertVentetid")}
                    {...register("visEstimertVentetid")}
                  >
                    Registrer estimert ventetid
                  </Switch>
                  {watch("visEstimertVentetid") ? (
                    <HStack align="start" justify="start" gap="10">
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
              </>
            ) : null}
          </FormGroup>
          <Separator />
          <FormGroup>
            <ControlledMultiSelect
              size="small"
              placeholder={isLoadingAnsatt ? "Laster..." : "Velg en"}
              label={tiltaktekster.administratorerForGjennomforingenLabel}
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
                label={tiltaktekster.navRegionLabel}
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
                label={tiltaktekster.navEnheterKontorerLabel}
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
                        onClick={() => removeKontaktperson(index)}
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
                          label={tiltaktekster.kontaktpersonNav.navnLabel}
                          {...register(`kontaktpersoner.${index}.navIdent`, {
                            shouldUnregister: true,
                          })}
                          options={kontaktpersonerOption(index)}
                        />
                        <ControlledMultiSelect
                          size="small"
                          placeholder={
                            isLoadingKontaktpersoner
                              ? "Laster enheter..."
                              : "Velg ett eller flere områder"
                          }
                          label={tiltaktekster.kontaktpersonNav.omradeLabel}
                          {...register(`kontaktpersoner.${index}.navEnheter`, {
                            shouldUnregister: true,
                          })}
                          options={navEnheterOptions.filter((enhet) =>
                            valgteNavEnheter.includes(enhet.value),
                          )}
                        />
                        <TextField
                          size="small"
                          label={tiltaktekster.kontaktpersonNav.beskrivelseLabel}
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
                  onClick={() => appendKontaktperson({} as TiltaksgjennomforingKontaktperson)}
                >
                  <PlusIcon aria-label="Legg til ny kontaktperson" /> Legg til ny kontaktperson
                </Button>
              </div>
            </FormGroup>
          </div>
          <div className={skjemastyles.gray_container}>
            <TiltaksgjennomforingArrangorSkjema readOnly={eierIkkeGjennomforing} avtale={avtale} />
          </div>
          {enableTilgjengeligForArrangor && watch("oppstart") === "LOPENDE" ? (
            <TiltakTilgjengeligForArrangor
              gjennomforingStartdato={new Date(watch("startOgSluttDato.startDato"))}
              lagretDatoForTilgjengeligForArrangor={
                tiltaksgjennomforing?.tilgjengeligForArrangorFraOgMedDato
              }
            />
          ) : null}
        </div>
      </div>
      <EndreDatoAdvarselModal
        modalRef={endreStartDatoModalRef}
        onCancel={() => setValue("startOgSluttDato.startDato", tiltaksgjennomforing!!.startDato)}
        antallDeltakere={deltakerSummary?.antallDeltakere ?? 0}
      />
      <EndreDatoAdvarselModal
        modalRef={endreSluttDatoModalRef}
        onCancel={() => setValue("startOgSluttDato.sluttDato", tiltaksgjennomforing!!.sluttDato)}
        antallDeltakere={deltakerSummary?.antallDeltakere ?? 0}
      />
    </div>
  );
};

function VelgUtdanningskategori() {
  const options = mockNusData.data[0].kategorier.map((kategori) => ({
    label: kategori.name,
    value: kategori.code,
  }));
  return (
    <UNSAFE_Combobox
      options={options}
      isMultiSelect
      size="small"
      label="Velg utdanningskategori"
    ></UNSAFE_Combobox>
  );
}
