import { Button, Textarea, TextField } from "@navikt/ds-react";
import {
  Avtale,
  AvtaleRequest,
  Avtalestatus,
  Avtaletype,
  EmbeddedTiltakstype,
  NavAnsatt,
  NavEnhet,
  NavEnhetType,
  Opphav,
  Tiltakskode,
  Tiltakstype,
} from "mulighetsrommet-api-client";
import { useEffect, useRef, useState } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import { useHentBetabrukere } from "../../api/ansatt/useHentBetabrukere";
import { useUpsertAvtale } from "../../api/avtaler/useUpsertAvtale";
import { useSokVirksomheter } from "../../api/virksomhet/useSokVirksomhet";
import { useVirksomhet } from "../../api/virksomhet/useVirksomhet";
import { addYear } from "../../utils/Utils";
import { Separator } from "../detaljside/Metadata";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FraTilDatoVelger } from "../skjema/FraTilDatoVelger";
import skjemastyles from "../skjema/Skjema.module.scss";
import { VirksomhetKontaktpersoner } from "../virksomhet/VirksomhetKontaktpersoner";
import { AvtaleSchema, InferredAvtaleSchema } from "./AvtaleSchema";

import { zodResolver } from "@hookform/resolvers/zod";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common/components/ControlledSokeSelect";
import { SelectOption } from "mulighetsrommet-frontend-common/components/SokeSelect";
import { MultiValue } from "react-select";
import { useHandleApiUpsertResponse } from "../../api/effects";
import { erAnskaffetTiltak } from "../../utils/tiltakskoder";
import { AvbrytAvtaleModal } from "../modal/AvbrytAvtaleModal";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { FormGroup } from "../skjema/FormGroup";
import {
  getLokaleUnderenheterAsSelectOptions,
  underenheterOptions,
  defaultAvtaleData,
} from "./AvtaleSkjemaConst";
import { AvtaleSkjemaKnapperad } from "./AvtaleSkjemaKnapperad";

const minStartdato = new Date(2000, 0, 1);

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
  const [sokLeverandor, setSokLeverandor] = useState(avtale?.leverandor?.organisasjonsnummer || "");
  const avbrytModalRef = useRef<HTMLDialogElement>(null);
  const mutation = useUpsertAvtale();
  const { data: betabrukere } = useHentBetabrukere();

  const { data: leverandorVirksomheter = [] } = useSokVirksomheter(sokLeverandor);

  const form = useForm<InferredAvtaleSchema>({
    resolver: zodResolver(AvtaleSchema),
    defaultValues: defaultAvtaleData(ansatt, avtale),
  });

  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
    setValue,
  } = form;

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
    const requestBody: AvtaleRequest = {
      id: avtale?.id ?? uuidv4(),
      navEnheter: data.navEnheter.concat(data.navRegioner),
      avtalenummer: avtale?.avtalenummer || null,
      leverandorOrganisasjonsnummer: data.leverandor,
      leverandorUnderenheter: data.leverandorUnderenheter,
      navn: data.navn,
      sluttDato: data.startOgSluttDato.sluttDato,
      startDato: data.startOgSluttDato.startDato,
      tiltakstypeId: data.tiltakstype.id,
      url: data.url || null,
      administratorer: data.administratorer,
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

  const { startDato } = watch("startOgSluttDato");
  const sluttDatoFraDato = startDato ? new Date(startDato) : minStartdato;
  const sluttDatoTilDato = addYear(startDato ? new Date(startDato) : new Date(), 5);

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <AvtaleSkjemaKnapperad redigeringsModus={redigeringsModus!} onClose={onClose} />
        <Separator classname={skjemastyles.avtaleskjema_separator} />
        <div className={skjemastyles.container}>
          <div className={skjemastyles.input_container}>
            <div className={skjemastyles.column}>
              <FormGroup cols={avtale?.avtalenummer ? 2 : 1}>
                <TextField
                  size="small"
                  readOnly={arenaOpphav}
                  error={errors.navn?.message}
                  label="Avtalenavn"
                  autoFocus
                  {...register("navn")}
                />
                {avtale?.avtalenummer ? (
                  <TextField
                    size="small"
                    readOnly
                    label="Avtalenummer"
                    value={avtale.avtalenummer}
                  />
                ) : null}
              </FormGroup>
              <Separator />
              <FormGroup cols={2}>
                <ControlledSokeSelect
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
                <ControlledSokeSelect
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
                    label: "Startdato",
                    readOnly: arenaOpphav,
                    fromDate: minStartdato,
                    toDate: sluttDatoTilDato,
                    ...register("startOgSluttDato.startDato"),
                    format: "iso-string",
                  }}
                  til={{
                    label: "Sluttdato",
                    readOnly: arenaOpphav,
                    fromDate: sluttDatoFraDato,
                    toDate: sluttDatoTilDato,
                    ...register("startOgSluttDato.sluttDato"),
                    format: "iso-string",
                  }}
                />
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
                <ControlledMultiSelect
                  size="small"
                  helpText="Bestemmer hvem som eier avtalen. Notifikasjoner sendes til administratorene."
                  placeholder="Administratorer"
                  label="Administratorer for avtalen"
                  {...register("administratorer")}
                  options={AdministratorOptions(ansatt, avtale?.administratorer, betabrukere)}
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
                    helpText="Bestemmer hvilke NAV-enheter som kan velges i gjennomføringene til avtalen."
                    {...register("navEnheter")}
                    options={getLokaleUnderenheterAsSelectOptions(watch("navRegioner"), enheter)}
                  />
                </FormGroup>
              </div>
              <div className={skjemastyles.gray_container}>
                <FormGroup>
                  <ControlledSokeSelect
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
                    helpText="Bestemmer hvilke arrangører som kan velges i gjennomføringene til avtalen."
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
