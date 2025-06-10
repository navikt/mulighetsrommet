import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useSokNavAnsatt } from "@/api/ansatt/useSokNavAnsatt";
import { useGjennomforingAdministratorer } from "@/api/ansatt/useGjennomforingAdministratorer";
import { useGjennomforingDeltakerSummary } from "@/api/gjennomforing/useGjennomforingDeltakerSummary";
import { GjennomforingAmoKategoriseringForm } from "@/components/amoKategorisering/GjennomforingAmoKategoriseringForm";
import { KontaktpersonButton } from "@/components/kontaktperson/KontaktpersonButton";
import { InferredGjennomforingSchema } from "@/components/redaksjoneltInnhold/GjennomforingSchema";
import { FormGroup } from "@/components/skjema/FormGroup";
import { SkjemaKolonne } from "@/components/skjema/SkjemaKolonne";
import { addYear, formaterDato, isKursTiltak } from "@/utils/Utils";
import {
  AvtaleDto,
  GjennomforingDto,
  GjennomforingKontaktperson,
  GjennomforingOppstartstype,
  NavEnhet,
  Tiltakskode,
} from "@mr/api-client-v2";
import { ControlledSokeSelect } from "@mr/frontend-common";
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
import { useEffect, useRef, useState } from "react";
import { useFieldArray, useFormContext } from "react-hook-form";
import { gjennomforingTekster } from "../ledetekster/gjennomforingLedetekster";
import { EndreDatoAdvarselModal } from "../modal/EndreDatoAdvarselModal";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { ControlledDateInput } from "../skjema/ControlledDateInput";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { GjennomforingUtdanningslopForm } from "../utdanning/GjennomforingUtdanningslopForm";
import { SelectOppstartstype } from "./SelectOppstartstype";
import { GjennomforingArrangorForm } from "./GjennomforingArrangorForm";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { AvtaleErUtkastOgArrangorManglerMelding } from "@/pages/avtaler/AvtaleDetaljer";
import {
  chooseAllUnderenheterBy,
  NavEnhetFilterType,
  splitNavEnheterByType,
} from "../navEnheter/helper";

interface Props {
  gjennomforing?: GjennomforingDto;
  avtale: AvtaleDto;
  enheter: NavEnhet[];
}

export function GjennomforingFormDetaljer({ gjennomforing, avtale, enheter }: Props) {
  const { data: administratorer } = useGjennomforingAdministratorer();
  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();

  const { data: deltakerSummary } = useGjennomforingDeltakerSummary(gjennomforing?.id);

  const endreSluttDatoModalRef = useRef<HTMLDialogElement>(null);

  const {
    register,
    control,
    formState: { errors },
    setValue,
    watch,
  } = useFormContext<InferredGjennomforingSchema>();

  const {
    fields: kontaktpersonFields,
    append: appendKontaktperson,
    remove: removeKontaktperson,
  } = useFieldArray({
    name: "kontaktpersoner",
    control,
  });

  const watchVisEstimertVentetid = watch("visEstimertVentetid");
  const navRegioner = watch("navRegioner");

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
    if (watchStartDato && new Date(watchStartDato) < new Date()) {
      setValue("tilgjengeligForArrangorDato", null);
    }
  }, [setValue, watchStartDato]);

  const watchSluttDato = watch("startOgSluttDato.sluttDato");

  function visAdvarselForSluttDato() {
    if (
      gjennomforing &&
      antallDeltakere &&
      antallDeltakere > 0 &&
      watchSluttDato &&
      gjennomforing.sluttDato !== watchSluttDato
    ) {
      endreSluttDatoModalRef.current?.showModal();
    }
  }

  const regionerOptions = avtale.kontorstruktur
    .map((struk) => struk.region)
    .map((kontor) => ({ value: kontor.enhetsnummer, label: kontor.navn }));

  const { navKontorEnheter, navAndreEnheter } = splitNavEnheterByType(
    avtale.kontorstruktur
      .flatMap((struk) => struk.kontorer)
      .filter((kontor) => navRegioner?.includes(kontor.overordnetEnhet ?? "")),
  );
  const navKontorEnheterOptions = navKontorEnheter.map((enhet) => ({
    label: enhet.navn,
    value: enhet.enhetsnummer,
  }));
  const navAndreEnheterOptions = navAndreEnheter.map((enhet) => ({
    label: enhet.navn,
    value: enhet.enhetsnummer,
  }));

  const minStartdato = new Date(avtale.startDato);
  const maxSluttdato = addYear(minStartdato, 35);

  return (
    <>
      <TwoColumnGrid separator>
        <SkjemaKolonne>
          <FormGroup>
            <TextField
              size="small"
              error={errors.navn?.message as string}
              label={gjennomforingTekster.tiltaksnavnLabel}
              autoFocus
              {...register("navn")}
            />
            {gjennomforing?.tiltaksnummer ? (
              <TextField
                size="small"
                readOnly
                label={gjennomforingTekster.tiltaksnummerLabel}
                autoFocus
                value={gjennomforing?.tiltaksnummer}
              />
            ) : null}
          </FormGroup>

          <FormGroup>
            <TextField
              size="small"
              readOnly
              label={gjennomforingTekster.avtaleMedTiltakstype(avtale.tiltakstype.navn)}
              value={avtale.navn || ""}
            />
            {errors.avtaleId?.message ? (
              <Alert variant="warning">{errors.avtaleId.message as string}</Alert>
            ) : null}
            {avtale.tiltakstype.tiltakskode === Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING ? (
              <GjennomforingAmoKategoriseringForm avtale={avtale} />
            ) : null}
            {avtale.tiltakstype.tiltakskode === Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING ? (
              <GjennomforingUtdanningslopForm avtale={avtale} />
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
                  label={gjennomforingTekster.avtaleStartdatoLabel}
                  readOnly
                  size="small"
                />
              </DatePicker>
              {avtale.sluttDato ? (
                <DatePicker>
                  <DatePicker.Input
                    value={formaterDato(avtale.sluttDato)}
                    label={gjennomforingTekster.avtaleSluttdatoLabel}
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
                label={gjennomforingTekster.startdatoLabel}
                fromDate={minStartdato}
                toDate={maxSluttdato}
                {...register("startOgSluttDato.startDato")}
                format={"iso-string"}
                control={control}
              />
              <ControlledDateInput
                size="small"
                label={gjennomforingTekster.sluttdatoLabel}
                fromDate={minStartdato}
                toDate={maxSluttdato}
                {...register("startOgSluttDato.sluttDato", {
                  onChange: visAdvarselForSluttDato,
                })}
                format={"iso-string"}
                control={control}
              />
            </HGrid>
            <HGrid align="start" columns={2}>
              <TextField
                size="small"
                error={errors.antallPlasser?.message as string}
                type="number"
                style={{ width: "180px" }}
                label={gjennomforingTekster.antallPlasserLabel}
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
                  label={gjennomforingTekster.deltidsprosentLabel}
                  {...register("deltidsprosent", {
                    valueAsNumber: true,
                  })}
                />
              )}
            </HGrid>
            {watch("oppstart") === GjennomforingOppstartstype.LOPENDE ? (
              <>
                <fieldset className="border-none p-0 [&>legend]:font-bold [&>legend]:mb-2">
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
              label={gjennomforingTekster.administratorerForGjennomforingenLabel}
              helpText="Bestemmer hvem som eier gjennomføringen. Notifikasjoner sendes til administratorene."
              {...register("administratorer")}
              options={AdministratorOptions(
                ansatt,
                gjennomforing?.administratorer,
                administratorer,
              )}
            />
          </FormGroup>
        </SkjemaKolonne>
        <SkjemaKolonne>
          <div>
            <FormGroup>
              <ControlledMultiSelect
                inputId={"navRegioner"}
                size="small"
                label={gjennomforingTekster.tilgjengeligIModiaLabel}
                placeholder="Velg en"
                {...register("navRegioner")}
                velgAlle
                options={regionerOptions}
                additionalOnChange={(selectedOptions) => {
                  if ((watch("navRegioner")?.length ?? 0) > 1) {
                    const alleLokaleUnderenheter = chooseAllUnderenheterBy(
                      selectedOptions,
                      enheter,
                      NavEnhetFilterType.LOKAL,
                    );
                    setValue("navEnheter", alleLokaleUnderenheter as [string, ...string[]]);
                  } else {
                    const alleLokaleUnderenheter = chooseAllUnderenheterBy(
                      selectedOptions,
                      enheter,
                      NavEnhetFilterType.LOKAL,
                    );
                    const navEnheter = watch("navEnheter")?.filter((enhet) =>
                      alleLokaleUnderenheter.includes(enhet ?? ""),
                    );
                    setValue("navEnheter", navEnheter as [string, ...string[]]);
                  }
                }}
              />
              <ControlledMultiSelect
                inputId={"navEnheter"}
                size="small"
                velgAlle
                placeholder={"Velg en"}
                label={gjennomforingTekster.navEnheterKontorerLabel}
                helpText="Bestemmer hvem gjennomføringen skal vises til i Modia, basert på hvilket kontor brukeren har tilhørighet til."
                {...register("navEnheter")}
                options={navKontorEnheterOptions}
              />
              <ControlledMultiSelect
                inputId={"navEnheterAndre"}
                size="small"
                velgAlle
                placeholder={"Velg en (valgfritt)"}
                label={gjennomforingTekster.navEnheterAndreLabel}
                helpText="Bestemmer hvem gjennomføringen skal vises til i Modia, basert på hvilke andre kontorer brukeren har tilhørighet til."
                {...register("navEnheterAndre")}
                options={navAndreEnheterOptions}
              />
            </FormGroup>

            <FormGroup>
              <div>
                {kontaktpersonFields?.map((field, index) => {
                  return (
                    <div
                      className="bg-surface-selected mt-4 p-2 relative border border-border-divider rounded"
                      key={field.id}
                    >
                      <Button
                        className="p-0 float-right"
                        variant="tertiary"
                        size="small"
                        type="button"
                        onClick={() => removeKontaktperson(index)}
                      >
                        <XMarkIcon fontSize="1.5rem" />
                      </Button>
                      <div className="flex flex-col gap-4">
                        <SokEtterKontaktperson
                          index={index}
                          navEnheter={navKontorEnheterOptions}
                          id={field.id}
                          lagredeKontaktpersoner={gjennomforing?.kontaktpersoner ?? []}
                        />
                      </div>
                    </div>
                  );
                })}
                <KontaktpersonButton
                  onClick={() =>
                    appendKontaktperson({
                      navIdent: "",
                      navEnheter: [],
                      beskrivelse: "",
                    })
                  }
                  knappetekst={
                    <div className="flex items-center gap-2">
                      <PlusIcon aria-label="Legg til ny kontaktperson" />
                      Legg til ny kontaktperson
                    </div>
                  }
                />
              </div>
            </FormGroup>
          </div>
          {avtale.arrangor ? (
            <FormGroup>
              <GjennomforingArrangorForm readOnly={false} arrangor={avtale.arrangor} />
            </FormGroup>
          ) : (
            <AvtaleErUtkastOgArrangorManglerMelding />
          )}
        </SkjemaKolonne>
      </TwoColumnGrid>
      <EndreDatoAdvarselModal
        modalRef={endreSluttDatoModalRef}
        onCancel={() => setValue("startOgSluttDato.sluttDato", gjennomforing!.sluttDato)}
        antallDeltakere={deltakerSummary?.antallDeltakere ?? 0}
      />
    </>
  );
}

function SokEtterKontaktperson({
  index,
  navEnheter,
  id,
  lagredeKontaktpersoner,
}: {
  index: number;
  navEnheter: { label: string; value: string }[];
  id: string;
  lagredeKontaktpersoner: GjennomforingKontaktperson[];
}) {
  const [kontaktpersonerQuery, setKontaktpersonerQuery] = useState<string>("");
  const { data: kontaktpersoner } = useSokNavAnsatt(kontaktpersonerQuery, id);
  const { register, watch } = useFormContext<InferredGjennomforingSchema>();

  const kontaktpersonerOption = (selectedIndex: number) => {
    const excludedKontaktpersoner = watch("kontaktpersoner")?.map((k) => k.navIdent);

    const alleredeValgt = watch("kontaktpersoner")
      ?.filter((_, i) => i === selectedIndex)
      ?.map((kontaktperson) => {
        const personFraSok = kontaktpersoner?.find((k) => k.navIdent == kontaktperson.navIdent);
        const personFraDb = lagredeKontaktpersoner?.find(
          (k) => k.navIdent == kontaktperson.navIdent,
        );
        const navn = personFraSok
          ? `${personFraSok?.fornavn} ${personFraSok?.etternavn}`
          : personFraDb?.navn;

        return {
          label: navn ? `${navn} - ${kontaktperson.navIdent}` : kontaktperson.navIdent,
          value: kontaktperson.navIdent,
        };
      });

    const options =
      kontaktpersoner
        ?.filter((kontaktperson) => !excludedKontaktpersoner?.includes(kontaktperson.navIdent))
        ?.map((kontaktperson) => ({
          label: `${kontaktperson.fornavn} ${kontaktperson.etternavn} - ${kontaktperson.navIdent}`,
          value: kontaktperson.navIdent,
        })) ?? [];

    return alleredeValgt ? [...alleredeValgt, ...options] : options;
  };

  const valgteNavEnheter = watch("navEnheter");

  return (
    <>
      <ControlledSokeSelect
        helpText="Bestemmer kontaktperson som veilederene kan hendvende seg til for informasjon om gjennomføringen. Kan gjelde for én eller flere enheter."
        size="small"
        placeholder="Søk etter kontaktperson"
        label={gjennomforingTekster.kontaktpersonNav.navnLabel}
        {...register(`kontaktpersoner.${index}.navIdent`, {
          shouldUnregister: true,
        })}
        onInputChange={setKontaktpersonerQuery}
        options={kontaktpersonerOption(index)}
      />
      <ControlledMultiSelect
        size="small"
        velgAlle
        placeholder="Velg ett eller flere områder"
        label={gjennomforingTekster.kontaktpersonNav.omradeLabel}
        {...register(`kontaktpersoner.${index}.navEnheter`, {
          shouldUnregister: true,
        })}
        options={navEnheter.filter((enhet) => valgteNavEnheter.includes(enhet.value))}
      />
      <TextField
        size="small"
        label={gjennomforingTekster.kontaktpersonNav.beskrivelseLabel}
        placeholder="Unngå personopplysninger"
        maxLength={67}
        {...register(`kontaktpersoner.${index}.beskrivelse`, {
          shouldUnregister: true,
        })}
      />
    </>
  );
}
