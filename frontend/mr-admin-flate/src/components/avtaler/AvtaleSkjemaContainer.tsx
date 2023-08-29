import { Alert, Textarea, TextField } from "@navikt/ds-react";
import {
  ApiError,
  Avtale,
  AvtaleRequest,
  Avtaletype,
  LeverandorUnderenhet,
  NavAnsatt,
  Norg2Type,
  Opphav,
  Utkast,
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
import { formaterDatoSomYYYYMMDD } from "../../utils/Utils";
import { AutoSaveUtkast } from "../autosave/AutoSaveUtkast";
import { Separator } from "../detaljside/Metadata";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FraTilDatoVelger } from "../skjema/FraTilDatoVelger";
import { SokeSelect } from "../skjema/SokeSelect";
import { VirksomhetKontaktpersoner } from "../virksomhet/VirksomhetKontaktpersoner";
import { AvtaleSchema, inferredAvtaleSchema } from "./AvtaleSchema";
import skjemastyles from "../skjema/Skjema.module.scss";

import {
  defaultEnhet,
  enheterOptions,
  erAnskaffetTiltak,
  getValueOrDefault,
  saveUtkast,
  underenheterOptions,
} from "./AvtaleSkjemaConst";
import { AnsvarligOptions } from "../skjema/AnsvarligOptions";
import { FormGroup } from "../skjema/FormGroup";
import { zodResolver } from "@hookform/resolvers/zod";
import { KnapperadOpprett } from "../skjema/KnapperadOpprett";
import { KnapperadRediger } from "../skjema/KnapperadRediger";
import { AvbrytAvtale } from "../skjemaknapper/AvbrytAvtale";
import { SlettUtkast } from "../skjemaknapper/SlettUtkast";

interface Props {
  onClose: () => void;
  onSuccess: (id: string) => void;
  tiltakstyper: Tiltakstype[];
  ansatt: NavAnsatt;
  avtale?: Avtale;
  utkast: Utkast;
  enheter: NavEnhet[];
  redigeringsmodus: boolean;
  utkastmodus: boolean;
}

export function AvtaleSkjemaContainer({
  onClose,
  onSuccess,
  tiltakstyper,
  ansatt,
  enheter,
  avtale,
  utkast,
  redigeringsmodus,
  utkastmodus,
}: Props) {
  const [navRegion, setNavRegion] = useState<string | undefined>(
    avtale?.navRegion?.enhetsnummer,
  );
  const [sokLeverandor, setSokLeverandor] = useState(
    avtale?.leverandor?.organisasjonsnummer || "",
  );

  const opprettAvtaleMutation = usePutAvtale();
  const { data: betabrukere } = useHentBetabrukere();
  const mutationUtkast = useMutateUtkast();
  const { data: leverandorVirksomheter = [] } =
    useSokVirksomheter(sokLeverandor);
  const utkastIdRef = useRef(avtale?.id || uuidv4());

  const form = useForm<inferredAvtaleSchema>({
    resolver: zodResolver(AvtaleSchema),
    defaultValues: {
      tiltakstype: avtale?.tiltakstype?.id,
      navRegion: defaultEnhet(avtale!, enheter, ansatt),
      navEnheter: avtale?.navEnheter?.map((e) => e.enhetsnummer) || [],
      avtaleansvarlig: avtale?.ansvarlig?.navident || ansatt.navIdent || "",
      avtalenavn: getValueOrDefault(avtale?.navn, ""),
      avtaletype: getValueOrDefault(avtale?.avtaletype, Avtaletype.AVTALE),
      leverandor: getValueOrDefault(
        avtale?.leverandor?.organisasjonsnummer,
        "",
      ),
      leverandorUnderenheter:
        avtale?.leverandorUnderenheter?.length === 0 ||
        !avtale?.leverandorUnderenheter
          ? []
          : avtale?.leverandorUnderenheter?.map(
              (leverandor: LeverandorUnderenhet) =>
                leverandor.organisasjonsnummer,
            ),
      leverandorKontaktpersonId: avtale?.leverandorKontaktperson?.id,
      startOgSluttDato: {
        startDato: avtale?.startDato ? new Date(avtale.startDato) : undefined,
        sluttDato: avtale?.sluttDato ? new Date(avtale.sluttDato) : undefined,
      },
      url: getValueOrDefault(avtale?.url, ""),
      prisOgBetalingsinfo: getValueOrDefault(
        avtale?.prisbetingelser,
        undefined,
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

  const getTiltakstypeFromId = (id: string): Tiltakstype | undefined => {
    return tiltakstyper.find((type) => type.id === id);
  };

  useEffect(() => {
    const arenaKode = getTiltakstypeFromId(watchedTiltakstype)?.arenaKode || "";
    if (["ARBFORB", "VASV"].includes(arenaKode)) {
      setValue("avtaletype", Avtaletype.FORHAANDSGODKJENT);
    }
  }, [watchedTiltakstype]);

  const { data: leverandorData } = useVirksomhet(watch("leverandor"));

  const underenheterForLeverandor = getValueOrDefault(
    leverandorData?.underenheter,
    [],
  );

  const arenaOpphav = avtale?.opphav === Opphav.ARENA;

  const postData: SubmitHandler<inferredAvtaleSchema> = async (
    data,
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
      id: utkastIdRef.current,
      navRegion,
      navEnheter,
      avtalenummer: getValueOrDefault(avtale?.avtalenummer, ""),
      leverandorOrganisasjonsnummer,
      leverandorUnderenheter,
      navn,
      sluttDato: formaterDatoSomYYYYMMDD(startOgSluttDato.sluttDato),
      startDato: formaterDatoSomYYYYMMDD(startOgSluttDato.startDato),
      tiltakstypeId,
      url,
      ansvarlig,
      avtaletype,
      prisOgBetalingsinformasjon: erAnskaffetTiltak(
        tiltakstypeId,
        getTiltakstypeFromId,
      )
        ? prisOgBetalingsinfo
        : undefined,
      opphav: avtale?.opphav,
      leverandorKontaktpersonId,
    };

    if (avtale?.id) {
      requestBody.id = avtale.id; // Ved oppdatering av eksisterende avtale
    }

    opprettAvtaleMutation.mutate(requestBody);
  };

  if (opprettAvtaleMutation.isSuccess) {
    onSuccess(opprettAvtaleMutation.data.id);
  }

  if (opprettAvtaleMutation.isError) {
    return (
      <Alert variant="error">
        {(opprettAvtaleMutation.error as ApiError).status === 400
          ? (opprettAvtaleMutation.error as ApiError).body
          : "Avtalen kunne ikke opprettes på grunn av en teknisk feil hos oss. " +
            "Forsøk på nytt eller ta <a href={PORTEN}>kontakt i Porten</a> dersom " +
            "du trenger mer hjelp."}
      </Alert>
    );
  }

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <div className={skjemastyles.container}>
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
                {redigeringsmodus && !utkastmodus ? (
                  <AvbrytAvtale handleAvbrytAvtale={onClose} />
                ) : null}
                {utkastmodus ? (
                  <SlettUtkast utkast={utkast!} handleDelete={onClose} />
                ) : null}
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
              {erAnskaffetTiltak(watch("tiltakstype"), getTiltakstypeFromId) ? (
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
                  description="Den som blir satt som ansvarlig vil få en notifikasjon."
                  options={AnsvarligOptions(
                    ansatt,
                    avtale?.ansvarlig,
                    betabrukere,
                  )}
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
                    options={enheterOptions(navRegion!, enheter)}
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
                    options={underenheterOptions(underenheterForLeverandor)}
                  />
                </FormGroup>
                {watch("leverandor") && !avtale?.leverandor?.slettet && (
                  <FormGroup>
                    <div className={skjemastyles.kontaktperson_container}>
                      <VirksomhetKontaktpersoner
                        title="Kontaktperson hos leverandøren"
                        orgnr={watch("leverandor")}
                        formValueName="leverandorKontaktpersonId"
                      />
                    </div>
                  </FormGroup>
                )}
              </div>
            </div>
          </div>
          <Separator />
          {redigeringsmodus ? (
            <KnapperadRediger
              opprettMutation={opprettAvtaleMutation}
              handleDelete={onClose}
              redigeringsmodus={redigeringsmodus}
              mutationUtkast={mutationUtkast}
              type="avtale"
              utkastmodus={utkastmodus}
            />
          ) : (
            <KnapperadOpprett
              opprettMutation={opprettAvtaleMutation}
              handleDelete={onClose}
              redigeringsmodus={redigeringsmodus}
              mutationUtkast={mutationUtkast}
              type="avtale"
            />
          )}
        </div>
      </form>
      <AutoSaveUtkast
        defaultValues={defaultValues}
        utkastId={utkastIdRef.current}
        onSave={() =>
          saveUtkast(watch(), avtale!, ansatt, utkastIdRef, mutationUtkast)
        }
        mutation={mutationUtkast}
      />
    </FormProvider>
  );
}
