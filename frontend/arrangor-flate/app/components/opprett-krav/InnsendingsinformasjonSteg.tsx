import {
  ArrangorflateTilsagnDto,
  DatoVelger,
  FieldError,
  GuidePanelType,
  OpprettKravInnsendingSteg,
  Periode,
  PeriodeType,
} from "@api-client";
import {
  BodyShort,
  DatePicker,
  GuidePanel,
  Heading,
  HStack,
  Label,
  Link,
  InfoCard,
  Select,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { SyntheticEvent, useCallback, useEffect, useMemo, useState } from "react";
import { OpprettKravFormState } from "~/routes/$orgnr.opprett-krav.$gjennomforingid";
import { filtrerOverlappendePerioder } from "~/utils/periode-filtrering";
import { TilsagnDetaljer } from "../tilsagn/TilsagnDetaljer";
import { LabeledDataElementList } from "../common/Definisjonsliste";
import { Link as ReactRouterLink } from "react-router";
import { errorAt } from "~/utils/validering";
import {
  formaterDato,
  formaterPeriode,
  isLaterOrSameDay,
  parseDate,
  subDuration,
  yyyyMMddFormatting,
} from "@mr/frontend-common/utils/date";
import { pathTo } from "~/utils/navigation";

interface InnsendingsinformasjonStepProps {
  data: OpprettKravInnsendingSteg;
  formState: OpprettKravFormState;
  updateFormState: (updates: Partial<OpprettKravFormState>) => void;
  errors: FieldError[];
}

interface DatoVelgerPeriodeChange {
  periodeStart?: string;
  periodeSlutt?: string;
  periodeType: PeriodeType;
}

export default function InnsendingsinformasjonSteg({
  data,
  formState,
  updateFormState,
  errors,
}: InnsendingsinformasjonStepProps) {
  const [fieldErrors, setFieldErrors] = useState(errors);
  const [valgtPeriode, setValgtPeriode] = useState<Periode | undefined>(() => {
    if (formState.periodeStart && formState.periodeSlutt) {
      return { start: formState.periodeStart, slutt: formState.periodeSlutt };
    }
    return undefined;
  });

  const relevanteTilsagn = useMemo(() => {
    if (!valgtPeriode) return [];
    return filtrerOverlappendePerioder<ArrangorflateTilsagnDto>(valgtPeriode, data.tilsagn);
  }, [data.tilsagn, valgtPeriode]);

  const handlePeriodeSelected = useCallback(
    ({ periodeStart, periodeSlutt, periodeType }: DatoVelgerPeriodeChange) => {
      setFieldErrors([]);
      const state = { periodeType, periodeStart, periodeSlutt };
      updateFormState(state);

      if (periodeStart && periodeSlutt) {
        const errors = validateDatoVelger(state, data.datoVelger);
        if (errors.length > 0) {
          setFieldErrors(errors);
        } else {
          setValgtPeriode({
            start: periodeStart!,
            slutt: periodeSlutt!,
          });
        }
      }
    },
    [updateFormState, setFieldErrors, data.datoVelger],
  );

  useEffect(() => {
    if (relevanteTilsagn.length > 0) {
      updateFormState({ tilsagnId: relevanteTilsagn[0].id });
    }
  }, [relevanteTilsagn, updateFormState]);

  return (
    <>
      <Heading level="3" size="large">
        Innsendingsinformasjon
      </Heading>
      <GuidePanelInformation type={data.guidePanel} />
      <VStack gap="space-16" className="max-w-2xl">
        <LabeledDataElementList entries={data.definisjonsListe} />
        <VStack gap="space-4">
          <Label size="small">Periode</Label>
          <BodyShort textColor="subtle" size="small">
            Hvilken periode gjelder kravet for?
          </BodyShort>
          <PeriodeVelgerVarianter
            onChange={handlePeriodeSelected}
            type={data.datoVelger}
            sessionPeriodeStart={formState.periodeStart}
            sessionPeriodeSlutt={formState.periodeSlutt}
            errors={fieldErrors}
            updateFormState={updateFormState}
          />
        </VStack>
        {valgtPeriode && (
          <>
            <Heading level="3" size="small">
              Tilgjengelige tilsagn
            </Heading>
            <BodyShort size="small" textColor="subtle">
              Under vises informasjon om antatt forbruk.
              <br />
              Hva som blir utbetalt avhenger imidlertid av faktisk forbruk i perioden.
            </BodyShort>
            {relevanteTilsagn.length < 1 ? (
              <InfoCard data-color="warning" size="small">
                <InfoCard.Header>
                  <InfoCard.Title>
                    Fant ingen aktive tilsagn for gjennomføringen. Vennligst ta kontakt med Nav.
                  </InfoCard.Title>
                </InfoCard.Header>
              </InfoCard>
            ) : (
              <>
                {relevanteTilsagn.map((tilsagn) => (
                  <TilsagnDetaljer key={tilsagn.id} tilsagn={tilsagn} minimal />
                ))}
              </>
            )}
          </>
        )}
      </VStack>
    </>
  );
}

interface GuidePanelInformationProps {
  type: GuidePanelType | null;
}

function GuidePanelInformation({ type }: GuidePanelInformationProps) {
  switch (type) {
    case GuidePanelType.INVESTERING_VTA_AFT:
      return (
        <GuidePanel>
          I dette skjemaet kan du sende inn krav som gjelder tilsagn for investeringer. Andre krav
          om utbetaling skal sendes inn via utbetalingene i{" "}
          <Link as={ReactRouterLink} to={pathTo.utbetalinger}>
            Utbetalingsoversikten.
          </Link>
        </GuidePanel>
      );
    case GuidePanelType.TIMESPRIS:
      return (
        <GuidePanel>
          I dette skjemaet kan du sende inn fakturakrav for tiltak med avtalt timespris
        </GuidePanel>
      );
    case GuidePanelType.AVTALT_PRIS:
      return (
        <GuidePanel>
          I dette skjemaet kan du sende inn fakturakrav i henhold til avtalt pris med Nav
        </GuidePanel>
      );
    case null:
      return null;
  }
}

interface PeriodeVelgerVarianterProps {
  onChange: (data: DatoVelgerPeriodeChange) => void;
  type: DatoVelger;
  sessionPeriodeStart?: string;
  sessionPeriodeSlutt?: string;
  errors?: FieldError[];
  updateFormState: (updates: Partial<OpprettKravFormState>) => void;
}

function PeriodeVelgerVarianter({
  onChange,
  type,
  sessionPeriodeStart,
  sessionPeriodeSlutt,
  errors,
}: PeriodeVelgerVarianterProps) {
  switch (type.type) {
    case "DatoVelgerSelect":
      return (
        <PeriodeSelect
          periodeForslag={type.periodeForslag}
          onChange={onChange}
          sessionPeriodeStart={sessionPeriodeStart}
          sessionPeriodeSlutt={sessionPeriodeSlutt}
        />
      );
    case "DatoVelgerRange":
      return (
        <PeriodeVelger
          maksSluttdato={type.maksSluttdato}
          onChange={onChange}
          sessionPeriodeStart={sessionPeriodeStart}
          sessionPeriodeSlutt={sessionPeriodeSlutt}
          errors={errors}
        />
      );
    case undefined:
      throw Error("Ugyldig DatoVelger variant");
  }
}

interface PeriodeSelectProps {
  onChange: (data: DatoVelgerPeriodeChange) => void;
  periodeForslag: Array<Periode>;
  sessionPeriodeStart?: string;
  sessionPeriodeSlutt?: string;
}

function PeriodeSelect({
  onChange: onPeriodeSelected,
  periodeForslag,
  sessionPeriodeStart,
  sessionPeriodeSlutt,
}: PeriodeSelectProps) {
  function onChange(e: SyntheticEvent<HTMLSelectElement, Event>) {
    const selectedValue = (e.target as HTMLSelectElement).value;
    if (!selectedValue) {
      onPeriodeSelected({ periodeType: PeriodeType.EKSKLUSIV });
      return;
    }
    const selectedPeriode = periodeForslag[Number(selectedValue)];
    onPeriodeSelected({
      periodeType: PeriodeType.EKSKLUSIV,
      periodeStart: selectedPeriode.start,
      periodeSlutt: selectedPeriode.slutt,
    });
  }

  return (
    <HStack gap="space-4">
      <Select
        hideLabel
        label="Hvilken periode gjelder kravet for?"
        size="small"
        name="periode"
        defaultValue={periodeForslag.findIndex(
          (periode) =>
            periode.start === sessionPeriodeStart && periode.slutt === sessionPeriodeSlutt,
        )}
        onChange={onChange}
      >
        <option value="">Velg periode</option>
        {periodeForslag.map((periode, index) => (
          <option key={periode.start} value={index}>
            {formaterPeriode(periode)}
          </option>
        ))}
      </Select>
    </HStack>
  );
}

interface PeriodeVelgerProps {
  maksSluttdato: string;
  onChange: (data: DatoVelgerPeriodeChange) => void;
  sessionPeriodeStart?: string;
  sessionPeriodeSlutt?: string;
  errors?: FieldError[];
}

function PeriodeVelger({
  maksSluttdato,
  onChange: onPeriodeSelected,
  sessionPeriodeStart,
  sessionPeriodeSlutt,
  errors,
}: PeriodeVelgerProps) {
  // Datepickeren returnerer bare gyldig Date objekter, og bare hhvis før `toDate`
  // Lagrer tilstanden seperat for å kunne gi valideringsmeldiner
  const [selectedStartDato, setSelectedStartDato] = useState(sessionPeriodeStart);
  const [selectedSluttDato, setSelectedSluttDato] = useState(sessionPeriodeSlutt);

  const sisteDato = subDuration(maksSluttdato, { days: 1 });
  const { datepickerProps: periodeStartPickerProps, inputProps: periodeStartInputProps } =
    useDatepicker({
      defaultSelected: parseDate(sessionPeriodeStart),
      toDate: sisteDato,
      onDateChange: (date) => setSelectedStartDato(yyyyMMddFormatting(date)),
    });
  const { datepickerProps: periodeSluttPickerProps, inputProps: periodeSluttInputProps } =
    useDatepicker({
      defaultSelected: parseDate(sessionPeriodeSlutt),
      toDate: sisteDato,
      onDateChange: (date) => setSelectedSluttDato(yyyyMMddFormatting(date)),
    });

  useEffect(() => {
    onPeriodeSelected({
      periodeType: PeriodeType.INKLUSIV,
      periodeStart: selectedStartDato,
      periodeSlutt: selectedSluttDato,
    });
  }, [selectedStartDato, selectedSluttDato, onPeriodeSelected, maksSluttdato]);

  return (
    <HStack wrap gap="space-4">
      <DatePicker
        {...periodeStartPickerProps}
        showWeekNumber
        dropdownCaption
        id="periodeStartDatepicker"
      >
        <DatePicker.Input
          {...periodeStartInputProps}
          label="Fra dato"
          size="small"
          error={errorAt("/periodeStart", errors)}
          name="periodeStart"
          id="periodeStart"
          onBlur={(e) => setSelectedStartDato(e.target.value)}
        />
      </DatePicker>
      <DatePicker
        {...periodeSluttPickerProps}
        showWeekNumber
        dropdownCaption
        id="periodeSluttDatepicker"
      >
        <DatePicker.Input
          {...periodeSluttInputProps}
          label="Til dato"
          size="small"
          error={errorAt("/periodeSlutt", errors)}
          name="periodeSlutt"
          id="periodeSlutt"
          onBlur={(e) => {
            setSelectedSluttDato(e.target.value);
          }}
        />
      </DatePicker>
    </HStack>
  );
}

interface InnsendingsinformasjonValidationContext {
  data: OpprettKravInnsendingSteg;
  formState: Partial<OpprettKravFormState>;
}
export const validateInnsendingsinformasjon = ({
  formState,
  data,
}: InnsendingsinformasjonValidationContext): FieldError[] => {
  const newErrors: FieldError[] = validateDatoVelger(formState, data.datoVelger);

  if (newErrors.length === 0 && !formState.tilsagnId) {
    newErrors.push({
      pointer: "/tilsagnId",
      detail: "Kan ikke opprette utbetalingskrav uten gyldig tilsagn",
    });
  }

  return newErrors;
};

function validateDatoVelger(
  formState: Partial<OpprettKravFormState>,
  datoVelger: DatoVelger,
): FieldError[] {
  const newErrors: FieldError[] = [];

  switch (datoVelger.type) {
    case "DatoVelgerRange": {
      newErrors.push(...validatePeriodeVelger(formState, datoVelger.maksSluttdato));
      break;
    }
    case "DatoVelgerSelect": {
      if (!formState.periodeStart) {
        newErrors.push({ pointer: "/periodeStart", detail: "Du må velge en periode" });
      }
      break;
    }
    case undefined:
      throw Error("undefined datoVelgerType");
  }
  return newErrors;
}

function validatePeriodeVelger(
  formState: Partial<OpprettKravFormState>,
  maksSluttdato: string,
): FieldError[] {
  const newErrors: FieldError[] = [];
  if (!formState.periodeStart) {
    newErrors.push({ pointer: "/periodeStart", detail: "Du må fylle ut fra dato" });
  }
  if (!formState.periodeSlutt) {
    newErrors.push({ pointer: "/periodeSlutt", detail: "Du må fylle ut til dato" });
  }
  if (isLaterOrSameDay(parseDate(formState.periodeStart), parseDate(formState.periodeSlutt))) {
    newErrors.push({
      pointer: "/periodeSlutt",
      detail: "Periodeslutt må være etter periodestart",
    });
  }
  if (isLaterOrSameDay(formState.periodeSlutt, maksSluttdato)) {
    newErrors.push({
      pointer: "/periodeSlutt",
      detail: `Periodeslutt må være før ${formaterDato(maksSluttdato)}`,
    });
  }
  return newErrors;
}
