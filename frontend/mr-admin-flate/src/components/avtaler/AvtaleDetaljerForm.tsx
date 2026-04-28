import { AvtaleAmoKategoriseringForm } from "@/components/amoKategorisering/AvtaleAmoKategoriseringForm";
import { AvtaleFormValues } from "@/schemas/avtale";
import { FormGroup } from "@/layouts/FormGroup";
import { LabelWithHelpText } from "@mr/frontend-common/components/label/LabelWithHelpText";
import { Box, HGrid, List, Select, TextField, UNSAFE_Combobox } from "@navikt/ds-react";
import { Controller, useFormContext } from "react-hook-form";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { AvtaleUtdanningslopForm } from "../utdanning/AvtaleUtdanningslopForm";
import { AvtaleArrangorForm } from "./AvtaleArrangorForm";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { AvtaleVarighet } from "./AvtaleVarighet";
import {
  Avtaletype,
  OpsjonLoggStatus,
  OpsjonsmodellType,
  PrismodellType,
  Rolle,
  Tiltakskode,
  Valuta,
} from "@tiltaksadministrasjon/api-client";
import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { useParams } from "react-router";
import { useTiltakstyperForAvtaler } from "@/api/tiltakstyper/useTiltakstyperForAvtaler";
import { erUtfaset } from "@/utils/tiltakstype";
import { SkjemaKolonne } from "@/layouts/SkjemaKolonne";
import { administratorOptions } from "@/components/skjema/administratorOptions";
import { useNavAnsatte } from "@/api/ansatt/useNavAnsatte";
import { SelectAvtaletype } from "@/components/avtaler/SelectAvtaletype";

export function AvtaleDetaljerForm() {
  const { avtaleId } = useParams();
  const { data: navAnsatte } = useNavAnsatte([Rolle.AVTALER_SKRIV]);
  const tiltakstyper = useTiltakstyperForAvtaler();
  const { data: avtale } = usePotentialAvtale(avtaleId ?? null);

  // Filtrer vekk utfasede tiltakstyper ved opprettelse, men ikke ved redigering
  const relevanteTiltakstyper = avtale
    ? tiltakstyper
    : tiltakstyper.filter((tiltakstype) => !erUtfaset(tiltakstype));

  const {
    register,
    formState: { errors },
    setValue,
    watch,
    control,
  } = useFormContext<AvtaleFormValues>();
  const tiltakskode = watch("detaljer.tiltakskode") as Tiltakskode | undefined;

  const antallOpsjonerUtlost = (
    avtale?.opsjonerRegistrert.filter((log) => log.status === OpsjonLoggStatus.OPSJON_UTLOST) || []
  ).length;

  function avtaletypeOnChange(avtaletype: Avtaletype) {
    if (avtaletype === Avtaletype.FORHANDSGODKJENT) {
      setValue("detaljer.opsjonsmodell", {
        type: OpsjonsmodellType.VALGFRI_SLUTTDATO,
        customOpsjonsmodellNavn: null,
        opsjonMaksVarighet: null,
      });

      setValue("prismodeller", []);
    } else {
      const prismodeller = watch("prismodeller");
      if (prismodeller.length === 0) {
        setValue("prismodeller", [
          {
            id: undefined,
            type: undefined as unknown as PrismodellType,
            valuta: Valuta.NOK,
            satser: [],
            prisbetingelser: null,
            tilsagnPerDeltaker: false,
          },
        ]);
      }
    }
  }

  return (
    <TwoColumnGrid separator>
      <SkjemaKolonne>
        <FormGroup>
          <TextField
            size="small"
            error={errors.detaljer?.navn?.message}
            label={avtaletekster.avtalenavnLabel}
            autoFocus
            {...register("detaljer.navn")}
          />
        </FormGroup>
        <FormGroup>
          <HGrid align="start" gap="space-16" columns={2}>
            <TextField
              size="small"
              placeholder="åå/12345"
              error={errors.detaljer?.sakarkivNummer?.message}
              label={
                <LabelWithHelpText
                  label={avtaletekster.sakarkivNummerLabel}
                  helpTextTitle={avtaletekster.sakarkivNummerHelpTextTitle}
                >
                  I Public 360 skal det opprettes tre typer arkivsaker med egne saksnummer:
                  <Box marginBlock="space-16" asChild>
                    <List data-aksel-migrated-v8>
                      <List.Item>En sak for hver anskaffelse.</List.Item>
                      <List.Item>
                        En sak for kontrakt/avtale med hver leverandør (Avtalesaken).
                      </List.Item>
                      <List.Item>
                        En sak for oppfølging og forvaltning av avtale (Avtaleforvaltningssaken).
                      </List.Item>
                    </List>
                  </Box>
                  Det er <b>2. Saksnummeret til Avtalesaken</b> som skal refereres til herfra.
                </LabelWithHelpText>
              }
              {...register("detaljer.sakarkivNummer")}
            />
          </HGrid>
        </FormGroup>
        <FormGroup>
          <HGrid gap="space-16" columns={2} align="start">
            <Select
              size="small"
              label={avtaletekster.tiltakstypeLabel}
              error={errors.detaljer?.tiltakskode?.message}
              {...register("detaljer.tiltakskode", {
                onChange: () => {
                  setValue("detaljer.amoKategorisering", null);
                  setValue("detaljer.utdanningslop", null);
                },
              })}
            >
              <option value="">-- Velg en --</option>
              {relevanteTiltakstyper.map((type) => (
                <option key={type.tiltakskode} value={type.tiltakskode}>
                  {type.navn}
                </option>
              ))}
            </Select>
            {tiltakskode && (
              <SelectAvtaletype
                tiltakskode={tiltakskode}
                readOnly={antallOpsjonerUtlost > 0}
                onChange={avtaletypeOnChange}
              />
            )}
          </HGrid>
          {tiltakskode && <AvtaleAmoKategoriseringForm tiltakskode={tiltakskode} />}
          {tiltakskode && <AvtaleUtdanningslopForm tiltakskode={tiltakskode} />}
        </FormGroup>
        <FormGroup>
          <AvtaleVarighet opsjonUtlost={antallOpsjonerUtlost > 0} />
        </FormGroup>
      </SkjemaKolonne>
      <SkjemaKolonne>
        <FormGroup>
          <Controller
            control={control}
            name="detaljer.administratorer"
            render={({ field }) => (
              <UNSAFE_Combobox
                size="small"
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
                selectedOptions={field.value.map(
                  (value) =>
                    administratorOptions(navAnsatte).find((o) => o.value === value) ?? {
                      value: value,
                      label: value,
                    },
                )}
                name={field.name}
                error={errors.detaljer?.administratorer?.message}
                options={administratorOptions(navAnsatte)}
                onToggleSelected={(option: string, isSelected: boolean) => {
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
      </SkjemaKolonne>
    </TwoColumnGrid>
  );
}
