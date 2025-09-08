import { useAvtaleAdministratorer } from "@/api/ansatt/useAvtaleAdministratorer";
import { AvtaleAmoKategoriseringForm } from "@/components/amoKategorisering/AvtaleAmoKategoriseringForm";
import { AvtaleFormValues } from "@/schemas/avtale";
import { FormGroup } from "@/components/skjema/FormGroup";
import { avtaletypeTilTekst } from "@/utils/Utils";
import {
  Avtaletype,
  OpsjonLoggRegistrert,
  OpsjonsmodellType,
  OpsjonStatus,
  Tiltakskode,
} from "@mr/api-client-v2";
import { LabelWithHelpText } from "@mr/frontend-common/components/label/LabelWithHelpText";
import { HGrid, List, Select, TextField, UNSAFE_Combobox, VStack } from "@navikt/ds-react";
import { Controller, useFormContext } from "react-hook-form";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { AvtaleUtdanningslopForm } from "../utdanning/AvtaleUtdanningslopForm";
import { AvtaleArrangorForm } from "./AvtaleArrangorForm";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { AvtaleVarighet } from "./AvtaleVarighet";

interface AvtaleDetaljerFormProps {
  opsjonerRegistrert?: OpsjonLoggRegistrert[];
}

export function AvtaleDetaljerForm({ opsjonerRegistrert }: AvtaleDetaljerFormProps) {
  const { data: administratorer } = useAvtaleAdministratorer();
  const { data: ansatt } = useHentAnsatt();
  const { data: tiltakstyper } = useTiltakstyper();

  const {
    register,
    formState: { errors },
    setValue,
    watch,
    control,
  } = useFormContext<AvtaleFormValues>();
  const tiltakskode = watch("tiltakskode");
  const watchedAdministratorer = watch("administratorer");

  const antallOpsjonerUtlost = (
    opsjonerRegistrert?.filter((log) => log.status === OpsjonStatus.OPSJON_UTLOST) || []
  ).length;

  const avtaletypeOptions = isTiltakskode(tiltakskode)
    ? getAvtaletypeOptions(tiltakskode).map((type) => (
        <option key={type.value} value={type.value}>
          {type.label}
        </option>
      ))
    : [];

  function avtaletypeOnChange(avtaletype: Avtaletype) {
    if (avtaletype === Avtaletype.FORHANDSGODKJENT) {
      setValue("opsjonsmodell", {
        type: OpsjonsmodellType.VALGFRI_SLUTTDATO,
        customOpsjonsmodellNavn: null,
        opsjonMaksVarighet: null,
      });
    }
  }

  return (
    <TwoColumnGrid separator>
      <VStack>
        <FormGroup>
          <TextField
            size="small"
            error={errors.navn?.message}
            label={avtaletekster.avtalenavnLabel}
            autoFocus
            {...register("navn")}
          />
        </FormGroup>
        <FormGroup>
          <HGrid align="start" gap="4" columns={2}>
            <TextField
              size="small"
              placeholder="åå/12345"
              error={errors.sakarkivNummer?.message}
              label={
                <LabelWithHelpText
                  label={avtaletekster.sakarkivNummerLabel}
                  helpTextTitle={avtaletekster.sakarkivNummerHelpTextTitle}
                >
                  I Public 360 skal det opprettes tre typer arkivsaker med egne saksnummer:
                  <List>
                    <List.Item>En sak for hver anskaffelse.</List.Item>
                    <List.Item>
                      En sak for kontrakt/avtale med hver leverandør (Avtalesaken).
                    </List.Item>
                    <List.Item>
                      En sak for oppfølging og forvaltning av avtale (Avtaleforvaltningssaken).
                    </List.Item>
                  </List>
                  Det er <b>2. Saksnummeret til Avtalesaken</b> som skal refereres til herfra.
                </LabelWithHelpText>
              }
              {...register("sakarkivNummer")}
            />
          </HGrid>
        </FormGroup>
        <FormGroup>
          <HGrid gap="4" columns={2} align="start">
            <Select
              size="small"
              label={avtaletekster.tiltakstypeLabel}
              error={errors.tiltakskode?.message}
              {...register("tiltakskode", {
                onChange: (e) => {
                  setValue("amoKategorisering", null);
                  setValue("utdanningslop", null);
                  setValue("prismodell", undefined as any, { shouldValidate: true });
                  setValue("satser", []);

                  const avtaletype = isTiltakskode(e.target.value)
                    ? getAvtaletypeOptions(e.target.value as Tiltakskode)[0]?.value
                    : undefined;
                  if (avtaletype) {
                    setValue("avtaletype", avtaletype);
                    avtaletypeOnChange(avtaletype);
                  }
                },
              })}
            >
              <option value="">-- Velg en --</option>
              {tiltakstyper.map((type) => (
                <option key={type.tiltakskode} value={type.tiltakskode ?? undefined}>
                  {type.navn}
                </option>
              ))}
            </Select>
            <Select
              size="small"
              readOnly={antallOpsjonerUtlost > 0}
              label={avtaletekster.avtaletypeLabel}
              error={errors.avtaletype?.message}
              {...register("avtaletype", {
                onChange: (e) => avtaletypeOnChange(e.target.value),
              })}
            >
              {avtaletypeOptions}
            </Select>
          </HGrid>
          {tiltakskode === Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING ? (
            <AvtaleAmoKategoriseringForm />
          ) : null}
          {tiltakskode === Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING ? (
            <AvtaleUtdanningslopForm />
          ) : null}
        </FormGroup>
        <FormGroup>
          <AvtaleVarighet antallOpsjonerUtlost={antallOpsjonerUtlost} />
        </FormGroup>
      </VStack>
      <VStack>
        <FormGroup>
          <Controller
            control={control}
            name="administratorer"
            render={({ field }) => (
              <UNSAFE_Combobox
                id="administratorer"
                label={
                  <LabelWithHelpText
                    label={avtaletekster.administratorerForAvtalenLabel}
                    helpTextTitle="Mer informasjon"
                  >
                    Bestemmer hvem som eier avtalen. Notifikasjoner sendes til administratorene.
                  </LabelWithHelpText>
                }
                placeholder="Administratorer"
                isMultiSelect
                selectedOptions={AdministratorOptions(
                  ansatt,
                  watchedAdministratorer,
                  administratorer,
                ).filter((option) => field.value.includes(option.value))}
                name={field.name}
                error={errors.administratorer?.message}
                options={AdministratorOptions(ansatt, watchedAdministratorer, administratorer)}
                onToggleSelected={(option, isSelected) => {
                  if (isSelected) {
                    field.onChange([...field.value, option]);
                  } else {
                    field.onChange(field.value.filter((v) => v !== option));
                  }
                }}
              />
            )}
          />
        </FormGroup>
        <AvtaleArrangorForm />
      </VStack>
    </TwoColumnGrid>
  );
}

function getAvtaletypeOptions(tiltakskode: Tiltakskode): { value: Avtaletype; label: string }[] {
  const forhandsgodkjent = {
    value: Avtaletype.FORHANDSGODKJENT,
    label: avtaletypeTilTekst(Avtaletype.FORHANDSGODKJENT),
  };
  const rammeavtale = {
    value: Avtaletype.RAMMEAVTALE,
    label: avtaletypeTilTekst(Avtaletype.RAMMEAVTALE),
  };
  const avtale = {
    value: Avtaletype.AVTALE,
    label: avtaletypeTilTekst(Avtaletype.AVTALE),
  };
  const offentligOffentlig = {
    value: Avtaletype.OFFENTLIG_OFFENTLIG,
    label: avtaletypeTilTekst(Avtaletype.OFFENTLIG_OFFENTLIG),
  };
  switch (tiltakskode) {
    case Tiltakskode.ARBEIDSFORBEREDENDE_TRENING:
    case Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET:
      return [forhandsgodkjent];
    case Tiltakskode.OPPFOLGING:
    case Tiltakskode.JOBBKLUBB:
    case Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK:
    case Tiltakskode.AVKLARING:
    case Tiltakskode.ARBEIDSRETTET_REHABILITERING:
      return [avtale, rammeavtale];
    case Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING:
    case Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING:
      return [avtale, offentligOffentlig, rammeavtale];
  }
}

function isTiltakskode(value: string): value is Tiltakskode {
  return Object.values(Tiltakskode).includes(value as Tiltakskode);
}
