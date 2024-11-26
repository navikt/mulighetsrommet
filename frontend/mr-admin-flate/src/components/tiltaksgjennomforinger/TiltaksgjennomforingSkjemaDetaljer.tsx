import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useTiltaksgjennomforingAdministratorer } from "@/api/ansatt/useTiltaksgjennomforingAdministratorer";
import { useGjennomforingDeltakerSummary } from "@/api/tiltaksgjennomforing/useTiltaksgjennomforingDeltakerSummary";
import { addYear, formaterDato } from "@/utils/Utils";
import { PlusIcon, XMarkIcon } from "@navikt/aksel-icons";
import {
  Alert,
  Button,
  DatePicker,
  HelpText,
  HGrid,
  HStack,
  Select,
  Switch,
  TextField,
} from "@navikt/ds-react";
import {
  AvtaleDto,
  TiltaksgjennomforingDto,
  TiltaksgjennomforingKontaktperson,
  TiltaksgjennomforingOppstartstype,
  Tiltakskode,
} from "@mr/api-client";
import { ControlledSokeSelect } from "@mr/frontend-common";
import { useEffect, useRef, useState } from "react";
import { useFieldArray, useFormContext } from "react-hook-form";
import { tiltaktekster } from "../ledetekster/tiltaksgjennomforingLedetekster";
import { EndreDatoAdvarselModal } from "../modal/EndreDatoAdvarselModal";
import { InferredTiltaksgjennomforingSchema } from "@/components/redaksjoneltInnhold/TiltaksgjennomforingSchema";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { ControlledDateInput } from "../skjema/ControlledDateInput";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FormGroup } from "@/components/skjema/FormGroup";
import { SelectOppstartstype } from "./SelectOppstartstype";
import { TiltaksgjennomforingArrangorSkjema } from "./TiltaksgjennomforingArrangorSkjema";
import { TiltaksgjennomforingAmoKategoriseringSkjema } from "@/components/amoKategorisering/TiltaksgjennomforingAmoKategoriseringSkjema";
import styles from "./TiltaksgjennomforingSkjemaDetaljer.module.scss";
import { SkjemaDetaljerContainer } from "@/components/skjema/SkjemaDetaljerContainer";
import { SkjemaInputContainer } from "@/components/skjema/SkjemaInputContainer";
import { SkjemaKolonne } from "@/components/skjema/SkjemaKolonne";
import { VertikalSeparator } from "@/components/skjema/VertikalSeparator";
import { KontaktpersonButton } from "@/components/kontaktperson/KontaktpersonButton";
import { isKursTiltak } from "@mr/frontend-common/utils/utils";
import { useSokNavAnsatt } from "@/api/ansatt/useSokNavAnsatt";
import { TiltaksgjennomforingUtdanningslopSkjema } from "../utdanning/TiltaksgjennomforingUtdanningslopSkjema";

interface Props {
  tiltaksgjennomforing?: TiltaksgjennomforingDto;
  avtale: AvtaleDto;
}

export function TiltaksgjennomforingSkjemaDetaljer({ tiltaksgjennomforing, avtale }: Props) {
  const { data: administratorer } = useTiltaksgjennomforingAdministratorer();
  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();
  const [kontaktpersonerQuery, setKontaktpersonerQuery] = useState<string>("");
  const { data: kontaktpersoner } = useSokNavAnsatt(kontaktpersonerQuery);
  const { data: deltakerSummary } = useGjennomforingDeltakerSummary(tiltaksgjennomforing?.id);

  const endreStartDatoModalRef = useRef<HTMLDialogElement>(null);
  const endreSluttDatoModalRef = useRef<HTMLDialogElement>(null);

  const kontaktpersonerOption = (selectedIndex: number) => {
    const excludedKontaktpersoner = watch("kontaktpersoner")
      ?.filter((_, i) => i !== selectedIndex)
      .map((k) => k.navIdent);

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
  }, [setValue, watchVisEstimertVentetid]);

  const watchStartDato = watch("startOgSluttDato.startDato");
  const antallDeltakere = deltakerSummary?.antallDeltakere;
  useEffect(() => {
    if (
      tiltaksgjennomforing &&
      antallDeltakere &&
      antallDeltakere > 0 &&
      tiltaksgjennomforing.startDato !== watchStartDato
    ) {
      endreStartDatoModalRef.current?.showModal();
    }
  }, [watchStartDato, antallDeltakere, tiltaksgjennomforing]);

  useEffect(() => {
    if (watchStartDato && new Date(watchStartDato) < new Date()) {
      setValue("tilgjengeligForArrangorFraOgMedDato", null);
    }
  }, [setValue, watchStartDato]);

  const watchSluttDato = watch("startOgSluttDato.sluttDato");
  useEffect(() => {
    if (
      tiltaksgjennomforing &&
      antallDeltakere &&
      antallDeltakere > 0 &&
      tiltaksgjennomforing.sluttDato !== watchSluttDato
    ) {
      endreSluttDatoModalRef.current?.showModal();
    }
  }, [watchSluttDato, antallDeltakere, tiltaksgjennomforing]);

  const regionerOptions = avtale.kontorstruktur
    .map((struk) => struk.region)
    .map((kontor) => ({ value: kontor.enhetsnummer, label: kontor.navn }));

  const navEnheterOptions = avtale.kontorstruktur
    .flatMap((struk) => struk.kontorer)
    .filter((kontor) => kontor.overordnetEnhet === watch("navRegion"))
    .map((kontor) => ({ label: kontor.navn, value: kontor.enhetsnummer }));

  const minStartdato = new Date(avtale.startDato);
  const maxSluttdato = addYear(minStartdato, 35);

  const valgteNavEnheter = watch("navEnheter");

  return (
    <SkjemaDetaljerContainer>
      <SkjemaInputContainer>
        <SkjemaKolonne>
          <FormGroup>
            <TextField
              size="small"
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
            {avtale.tiltakstype.tiltakskode === Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING ? (
              <TiltaksgjennomforingAmoKategoriseringSkjema avtale={avtale} />
            ) : null}
            {avtale.tiltakstype.tiltakskode === Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING ? (
              <TiltaksgjennomforingUtdanningslopSkjema avtale={avtale} />
            ) : null}
          </FormGroup>

          <FormGroup>
            <SelectOppstartstype
              name="oppstart"
              readonly={!isKursTiltak(avtale.tiltakstype.tiltakskode)}
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
                fromDate={minStartdato}
                toDate={maxSluttdato}
                {...register("startOgSluttDato.startDato")}
                format={"iso-string"}
              />
              <ControlledDateInput
                size="small"
                label={tiltaktekster.sluttdatoLabel}
                fromDate={minStartdato}
                toDate={maxSluttdato}
                {...register("startOgSluttDato.sluttDato")}
                format={"iso-string"}
              />
            </HGrid>
            <HGrid align="start" columns={2}>
              <TextField
                size="small"
                error={errors.antallPlasser?.message as string}
                type="number"
                style={{ width: "180px" }}
                label={tiltaktekster.antallPlasserLabel}
                {...register("antallPlasser", {
                  valueAsNumber: true,
                })}
              />
              {isKursTiltak(avtale.tiltakstype.tiltakskode) && (
                <TextField
                  size="small"
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
                <fieldset className={styles.fieldset_no_styling}>
                  <HStack gap="1">
                    <legend>Estimert ventetid</legend>
                    <HelpText title="Hva er estimert ventetid?">
                      Estimert ventetid er et felt som kan brukes hvis dere sitter på informasjon om
                      estimert ventetid for tiltaket. Hvis dere legger inn en verdi i feltene her
                      blir det synlig for alle ansatte i Nav.
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
        </SkjemaKolonne>
        <VertikalSeparator />
        <SkjemaKolonne>
          <div>
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
                velgAlle
                placeholder={"Velg en"}
                label={tiltaktekster.navEnheterKontorerLabel}
                helpText="Bestemmer hvem gjennomføringen skal vises til i Modia, basert på hvilket kontor brukeren har tilhørighet til."
                {...register("navEnheter")}
                options={navEnheterOptions}
              />
            </FormGroup>

            <FormGroup>
              <div>
                {kontaktpersonFields?.map((field, index) => {
                  return (
                    <div className={styles.kontaktperson_container} key={field.id}>
                      <Button
                        className={styles.kontaktperson_fjern_button}
                        variant="tertiary"
                        size="small"
                        type="button"
                        onClick={() => removeKontaktperson(index)}
                      >
                        <XMarkIcon fontSize="1.5rem" />
                      </Button>
                      <div className={styles.kontaktperson_inputs}>
                        <ControlledSokeSelect
                          helpText="Bestemmer kontaktperson som veilederene kan hendvende seg til for informasjon om gjennomføringen. Kan gjelde for én eller flere enheter."
                          size="small"
                          placeholder="Søk etter kontaktperson"
                          label={tiltaktekster.kontaktpersonNav.navnLabel}
                          {...register(`kontaktpersoner.${index}.navIdent`, {
                            shouldUnregister: true,
                          })}
                          onInputChange={(s: string) => {
                            setKontaktpersonerQuery(s);
                          }}
                          options={kontaktpersonerOption(index)}
                        />
                        <ControlledMultiSelect
                          size="small"
                          velgAlle
                          placeholder="Velg ett eller flere områder"
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
                <KontaktpersonButton
                  onClick={() => appendKontaktperson({} as TiltaksgjennomforingKontaktperson)}
                  knappetekst={
                    <>
                      <PlusIcon aria-label="Legg til ny kontaktperson" /> Legg til ny kontaktperson
                    </>
                  }
                />
              </div>
            </FormGroup>
          </div>
          <FormGroup>
            <TiltaksgjennomforingArrangorSkjema readOnly={false} avtale={avtale} />
          </FormGroup>
        </SkjemaKolonne>
      </SkjemaInputContainer>
      <EndreDatoAdvarselModal
        modalRef={endreStartDatoModalRef}
        onCancel={() => setValue("startOgSluttDato.startDato", tiltaksgjennomforing!.startDato)}
        antallDeltakere={deltakerSummary?.antallDeltakere ?? 0}
      />
      <EndreDatoAdvarselModal
        modalRef={endreSluttDatoModalRef}
        onCancel={() => setValue("startOgSluttDato.sluttDato", tiltaksgjennomforing!.sluttDato)}
        antallDeltakere={deltakerSummary?.antallDeltakere ?? 0}
      />
    </SkjemaDetaljerContainer>
  );
}
